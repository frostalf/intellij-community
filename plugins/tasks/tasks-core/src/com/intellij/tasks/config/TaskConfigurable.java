/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.tasks.config;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.binding.BindControl;
import com.intellij.openapi.options.binding.BindableConfigurable;
import com.intellij.openapi.options.binding.ControlBinder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.tasks.CommitPlaceholderProvider;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.impl.TaskManagerImpl;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ExpandableEditorSupport;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TaskConfigurable extends BindableConfigurable implements SearchableConfigurable.Parent, Configurable.NoScroll {

  private JPanel myPanel;

  @BindControl("updateEnabled")
  private JCheckBox myUpdateCheckBox;

  @BindControl("updateIssuesCount")
  private JTextField myUpdateCount;

  @BindControl("updateInterval")
  private JTextField myUpdateInterval;

  @BindControl("taskHistoryLength")
  private JTextField myHistoryLength;
  private JPanel myCacheSettings;

  @BindControl("saveContextOnCommit")
  private JCheckBox mySaveContextOnCommit;

  @BindControl("changelistNameFormat")
  private EditorTextField myChangelistNameFormat;

  private JBCheckBox myAlwaysDisplayTaskCombo;
  private JTextField myConnectionTimeout;

  @BindControl("branchNameFormat")
  private EditorTextField myBranchNameFormat;

  private final Project myProject;
  private Configurable[] myConfigurables;
  private final NotNullLazyValue<ControlBinder> myControlBinder = new NotNullLazyValue<ControlBinder>() {
    @NotNull
    @Override
    protected ControlBinder compute() {
      return new ControlBinder(getConfig());
    }
  };

  public TaskConfigurable(Project project) {
    super();
    myProject = project;
    myUpdateCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        enableCachePanel();
      }
    });
  }

  private TaskManagerImpl.Config getConfig() {
    return ((TaskManagerImpl)TaskManager.getManager(myProject)).getState();
  }

  @Override
  protected ControlBinder getBinder() {
    return myControlBinder.getValue();
  }

  private void enableCachePanel() {
    GuiUtils.enableChildren(myCacheSettings, myUpdateCheckBox.isSelected());
  }

  @Override
  public void reset() {
    super.reset();
    enableCachePanel();
    myAlwaysDisplayTaskCombo.setSelected(TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO);
    myConnectionTimeout.setText(Integer.toString(TaskSettings.getInstance().CONNECTION_TIMEOUT));
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myChangelistNameFormat.getText().trim().isEmpty()) {
      throw new ConfigurationException("Change list name format should not be empty");
    }
    if (myBranchNameFormat.getText().trim().isEmpty()) {
      throw new ConfigurationException("Branch name format should not be empty");
    }
    boolean oldUpdateEnabled = getConfig().updateEnabled;
    super.apply();
    TaskManager manager = TaskManager.getManager(myProject);
    if (getConfig().updateEnabled && !oldUpdateEnabled) {
      manager.updateIssues(null);
    }
    TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO = myAlwaysDisplayTaskCombo.isSelected();
    int oldConnectionTimeout = TaskSettings.getInstance().CONNECTION_TIMEOUT;
    Integer connectionTimeout = Integer.valueOf(myConnectionTimeout.getText());
    TaskSettings.getInstance().CONNECTION_TIMEOUT = connectionTimeout;

    if (connectionTimeout != oldConnectionTimeout) {
      for (TaskRepository repository : manager.getAllRepositories()) {
        if (repository instanceof BaseRepositoryImpl) {
          ((BaseRepositoryImpl)repository).reconfigureClient();
        }
      }
    }
  }

  @Override
  public boolean isModified() {
    return super.isModified() ||
           TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO != myAlwaysDisplayTaskCombo.isSelected() ||
      TaskSettings.getInstance().CONNECTION_TIMEOUT != Integer.valueOf(myConnectionTimeout.getText());
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Tasks";
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.project.tasks";
  }

  @Override
  public JComponent createComponent() {
    bindAnnotations();
    return myPanel;
  }

  @Override
  @NotNull
  public String getId() {
    return "tasks";
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @NotNull
  @Override
  public Configurable[] getConfigurables() {
    if (myConfigurables == null) {
      myConfigurables = new Configurable[] { new TaskRepositoriesConfigurable(myProject) };
    }
    return myConfigurables;
  }

  private void createUIComponents() {
    FileType fileType = FileTypeManager.getInstance().findFileTypeByName("VTL");
    if (fileType == null) {
      fileType = PlainTextFileType.INSTANCE;
    }
    Project project = ProjectManager.getInstance().getDefaultProject();
    myBranchNameFormat = new EditorTextField(project, fileType);
    setupAddAction(myBranchNameFormat);
    myChangelistNameFormat = new EditorTextField(project, fileType);
    setupAddAction(myChangelistNameFormat);
  }

  private void setupAddAction(EditorTextField field) {
    field.addSettingsProvider(editor -> {
      ExtendableTextComponent.Extension extension =
        ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Add placeholder", () -> {
          Set<String> placeholders = new HashSet<>();
          for (CommitPlaceholderProvider provider : CommitPlaceholderProvider.EXTENSION_POINT_NAME.getExtensionList()) {
            placeholders.addAll(Arrays.asList(provider.getPlaceholders(null)));
          }
          JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<String>("Placeholders", ArrayUtil.toStringArray(placeholders)) {
            @Override
            public PopupStep onChosen(String selectedValue, boolean finalChoice) {
              WriteCommandAction.runWriteCommandAction(myProject, () -> editor.getDocument()
                .insertString(editor.getCaretModel().getOffset(), "${" + selectedValue + "}"));
              return FINAL_CHOICE;
            }
          }).showInBestPositionFor(editor);
        });
      ExpandableEditorSupport.setupExtension(editor, field.getBackground(), extension);
    });
  }
}

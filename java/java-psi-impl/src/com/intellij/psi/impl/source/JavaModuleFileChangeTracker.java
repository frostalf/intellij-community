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
package com.intellij.psi.impl.source;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.psi.*;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.PsiTreeChangeEvent.*;

public class JavaModuleFileChangeTracker implements ModificationTracker {
  private static final NotNullLazyKey<ModificationTracker, Project> KEY = NotNullLazyKey.create("", new NotNullFunction<Project, ModificationTracker>() {
    @NotNull
    @Override
    public ModificationTracker fun(Project project) {
      return new JavaModuleFileChangeTracker(project);
    }
  });

  @NotNull
  public static ModificationTracker getInstance(@NotNull Project p) {
    return KEY.getValue(p);
  }

  private volatile long myCount = 0;

  private JavaModuleFileChangeTracker(Project project) {
    PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      @Override public void childAdded(@NotNull PsiTreeChangeEvent event) { process(event.getFile()); }
      @Override public void childRemoved(@NotNull PsiTreeChangeEvent event) { process(event.getFile()); }
      @Override public void childReplaced(@NotNull PsiTreeChangeEvent event) { process(event.getFile()); }
      @Override public void childMoved(@NotNull PsiTreeChangeEvent event) { process(event.getFile()); }
      @Override public void childrenChanged(@NotNull PsiTreeChangeEvent event) { process(event.getFile()); }

      private void process(PsiFile file) {
        if (file != null && PsiJavaModule.MODULE_INFO_FILE.equals(file.getName())) {
          myCount++;
        }
      }

      @Override
      public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        String name = event.getPropertyName();
        if (name == PROP_FILE_NAME || name == PROP_DIRECTORY_NAME || name == PROP_ROOTS) {
          myCount++;
        }
      }
    }, project);
  }

  @Override
  public long getModificationCount() {
    return myCount;
  }
}
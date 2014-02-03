from datetime import datetime
from datetime import date

class MainParent(object):
    pass

class SubParent1(MainParent):
    def foo(self):
        pass
    pass

class SubParent2(MainParent):
    pass

class Child(SubParent1, SubParent2):
    def spam(self):
        pass
    pass

class NoParentsAllowed(datetime, object):
    def foo(self):
        pass
    pass


class NoMembers(object):
    pass

class BadMro(MainParent, object, SubParent1, SubParent2):
    pass

class SomeMembersDisabled(SubParent1, date): #SubParent1 is disabled
    CLASS_FIELD = 42
    (CLASS_FIELD_A,CLASS_FIELD_B) = (42,100500) We do not support tuples in class assignments for now (see ClassFieldsManager)
    def foo(self): #should be disabled
        pass
    def bar(self):
        pass
package specs.race

import org.specs2.mutable.Specification
import specs.SpecUtil

class RaceCreateSpec extends Specification with SpecUtil { override def is = s2"""
  
  Story: Race Manager manage racing id spec
  
  As a race manager,
  I want to manage the racing ID parameter 
  so that i can manage to see the racing information in real time info
  
  Given a race manager account is created
  When a race manager try to create a race car at /race/settings
  Then a race car upload id and tag will be created
  
  Given a race manager account is created, and 3 race car has been created
  When a race manager rename the race car tag
  Then a race car name will be updated in the realtime dashboard
  
  Given a race manager account is created, and 3 race car has been created
  When a race manager disable the race
  Then the disabled car will be hidden in the race dashboard
  
  Given a race manager account is created, and 3 race car has been created
  When a race manager disable the race
  Then the disabled car will be hidden in the race dashboard
  
"""

  
  
  
}
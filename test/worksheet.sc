object worksheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  import helpers.java._
  
  TorqueFreeUtil.encode("353845051569987")        //> res0: String = cca6e349887eab3ace1aed9d0cb877b7
  // compare: cca6e349887eab3ace1aed9d0cb877b7
  // compare: cca6e349887eab3ace1aed9d0cb877b7
  
  //TorqueFreeUtil.decode("cca6e349887eab3ace1aed9d0cb877b7")
  //TorqueFreeUtil.decode("cca6e349887eab3ace1aed9d0cb877b7")
}
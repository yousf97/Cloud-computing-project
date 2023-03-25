// package com.UsefulPack

// import akka.actor.Actor
// import akka.actor.Props
// import akka.actor.ActorSystem


// class ValueStore extends Actor {
//     val myTable = HashMap[String, String]()
//     class store {
//         myTable(key) = value

//     }
//     def store(key: String, value: String): String = {
//     myTable(key) = value
//     }

//     def lookup(key: String): String = {
//     value = myTable.getOrElse(key, "Key not found")
//     value
//     }

//     def delete(key: String): Option[String] = {
//     myTable.remove(key)
//     }
//   }
// // class ConsoleReader extends Actor {

//     while (true){
//   println("Enter an operation (store, lookup, delete):")
//   val operation = readLine().toLowerCase()

//   operation match {
    
//     case "store" =>
//       println("Enter a key:")
//       val key = readLine()
//       println("Enter a value:")
//       val value = readLine()
//       val value = ValueStore.store(key, value)
//       println(s"Stored key $key with value $value")
//     case "lookup" =>
//       println("Enter a key:")
//       val key = readLine()
//       val value = ValueStore.lookup(key)
//       println(value)
//     case "delete" =>
//       println("Enter a key:")
//       val key = readLine()
//       val value = ValueStore.delete(key)
//       if (value.isDefined) {
//         println(s"Deleted key $key with value ${value.get}")
//       } else {
//         println("Key not found")
//       }
//   }
//   }
    
// }


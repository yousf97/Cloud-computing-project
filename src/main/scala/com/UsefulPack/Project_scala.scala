package com.UsefulPack

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.collection.mutable
import scala.io.StdIn.readLine
import scala.io.Source

import java.io.{File, FileWriter}
// Messages sent between actors
object Messages {
case class Store(key: String, value: String)
case class Lookup(key: String)
case class Delete(key: String)
case class Start(actorRef: ActorRef)

}



class CashActor(keyvalue: ActorRef) extends Actor {
  import Messages._

  val cache = mutable.LinkedHashMap.empty[String, String]

  def receive = {
    case Store(key, value) =>
      
      cache.put(key, value)
      keyvalue ! Store(key, value)

      // If the cache has exceeded its limit, remove the least recently used entry
      if (cache.size > 1000) {
        val (k, _) = cache.head
        cache.remove(k)
      }

    case Delete(key) =>
      
      cache.remove(key)
      
      keyvalue ! Delete(key)

    case Lookup(key) =>
      cache.get(key) match {
        case Some(value) =>
          // If the key is present in the cache, return the value from the cache and put the value in top of the cash
          cache.remove(key)
          cache.put(key, value)
          // sender() ! Some(value)
          
        case None =>
          // If the key is not present in the cache, forward the message to the file actor
          keyvalue ! Lookup(key)
        
          
     }
    
    }

}


class KeyValueActor(file: ActorRef) extends Actor {
    import Messages._

    val data = mutable.HashMap.empty[String, String]

    def receive = {
        case Store(key, value) =>
            Thread.sleep(1000)
            data.put(key, value)
            file ! Store(key, value)

        case Delete(key) =>
            Thread.sleep(1000)
            data.put(key, "deleted") // update the value as deleted
            file ! Delete(key)

        case Lookup(key) =>
            Thread.sleep(1000)
            data.get(key) match {
            case Some(value) =>
            // If the key is present in the key-value store, return the value from the data
                // sender() ! Some(value)
                println(s"$value")
            case None =>
            // If the key is not present in the key-value store, forward the message to the file actor
               file ! Lookup(key)
               

        }
        case Some(result) =>
          println(result)

        
    
  }

}

class FileActor extends Actor {
    import Messages._
  

  val filename = "store.txt"
  val writer = new FileWriter("store.txt", true)


  def receive = {
    case Store(key, value) =>
      
      writer.write(s"$key:$value \n")
      writer.flush()
      

    case Delete(key) =>
      
      writer.write(s"$key:deleted \n")
      writer.flush()
    case Lookup(key) =>
      val it = Source.fromFile(filename).getLines().map(_.split(":")).filter { case Array(k, _) => k == key }
      val result = it.find { case Array(_, v) => v != "deleted" }
      // sender() ! result.map { case Array(_, v) => v }.getOrElse(null)


      
  }
}

class ConsoleReader(store: ActorRef) extends Actor {
  import Messages._
  def receive = {
      
    
    case Start(actorRef) =>
        println("Enter store, lookup or delete:")
        val command = scala.io.StdIn.readLine()
        
    
        command match {
        
        case "store" =>
          println("Enter key:")
          val key = scala.io.StdIn.readLine()
          println("Enter value:")
          val value = scala.io.StdIn.readLine()
          store ! Store(key, value)
        case "lookup" =>
          println("Enter key:")
          val key = scala.io.StdIn.readLine()
          store ! Lookup(key)
          

        case "delete" =>
          println("Enter key:")
          val key = scala.io.StdIn.readLine()
          store ! Delete(key)
        
          
        case _ =>
          println("Invalid input")
      }
  self ! Start(store)
  }
  
}



import scala.util.Random
import akka.actor.ActorRef
import scala.concurrent.Future



class RandomClient(store: ActorRef) {
  import Messages._

  // Populate the storage with values for keys from 1-500
  
  
  // Initialize the last key looked-up to a random value between 1-500
  var lastKey = Random.nextInt(500) + 1

  // Define the probability of picking the last key looked-up for a lookup operation
  val lookupLastKeyProbability = 0.5

  // Define the probability of picking a key from the first 1-500 range for a lookup operation
  val lookupFirstKeysProbability = 0.75

  // Define the total number of operations to perform
  val totalOperations = 10000

  // Loop through the total number of operations and make random requests to the storage service
  for (i <- 1 to totalOperations) {
    // Pick a random operation from store, delete, and lookup
    val operation = Random.nextInt(3) match {
      case 0 => Store
      case 1 => Delete
      case 2 => Lookup
    }

    // Pick a random key
    val key = if (operation == Lookup && Random.nextDouble() < lookupLastKeyProbability) {
      lastKey
    } else {
      if (Random.nextDouble() < lookupFirstKeysProbability) {
        Random.nextInt(500) + 1
      } else {
        Random.nextInt(500) + 501
      }
    }

    // If the operation is a store or delete, make sure that the key picked is greater than 500
    val realKey = operation match {
      case Store | Delete => if (key < 501) key + 500 else key
      case _ => key
    }

    // Perform the operation
    operation match {
      case Store => store ! Store(realKey.toString, s"value$realKey")
      case Delete => store ! Delete(realKey.toString)
      case Lookup =>
        store ! Lookup(realKey.toString)
        lastKey = realKey
    }
  }

}
// To run the system against the version without the cache and the version with the cache, we can modify the Part22 object as follows:


object Part22 extends App {
  val system = ActorSystem("StoreSystem")
  val fileStore = system.actorOf(Props[FileActor], "FileActor")
  val keyValueStore = system.actorOf(Props(new KeyValueActor(fileStore)), "KeyValueActor")
  val cashStore = system.actorOf(Props(new CashActor(keyValueStore)), "CashActor")
  val consoleReader = system.actorOf(Props(new ConsoleReader(cashStore)), "consoleReader")
  // consoleReader ! Messages.Start(cashStore)


  for (i <- 1 to 500) {
    fileStore ! Messages.Store(i.toString, s"value$i")
  }
  
  
  Thread.sleep(50000)
  // Run the system without the cache
  val t1 = System.currentTimeMillis
  println("Running the system without the cache")
  val randomClientWithoutCache = new RandomClient(keyValueStore) // we begin directly by key_value actor
  val test1 = randomClientWithoutCache
  println(s"system without the cache took ${System.currentTimeMillis - t1} millis")
  Thread.sleep(10000)

  val t2 = System.currentTimeMillis
  // Run the system with the cache
  println("Running the system with the cache")
  val randomClientWithCache = new RandomClient(cashStore)
  val test2 = randomClientWithCache
  println(s"system with the cache took ${System.currentTimeMillis - t2} millis")
  // val f1 = Future(randomClientWithCache ! cashStore)
  Thread.sleep(10000)

  // Shutdown the system
  system.terminate()
}
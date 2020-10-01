package rai.script

import com.google.api.gax.rpc.NotFoundException
import com.google.cloud.recommendationengine.v1beta1.CatalogItemPathName
import com.google.cloud.recommendationengine.v1beta1.CatalogName
import com.google.cloud.recommendationengine.v1beta1.CatalogServiceClient
import com.google.cloud.recommendationengine.v1beta1.CatalogServiceSettings
import com.google.cloud.recommendationengine.v1beta1.DeleteCatalogItemRequest
import com.google.cloud.recommendationengine.v1beta1.EventStoreName
import com.google.cloud.recommendationengine.v1beta1.UserEventServiceClient
import com.google.cloud.recommendationengine.v1beta1.UserEventServiceSettings
import com.google.common.util.concurrent.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

fun deleteCatalog(args: Array<String>) = runBlocking {
  val catalogName = CatalogName.of("tikiandroid-1047", "global", "default_catalog")
  val parent = EventStoreName.of("tikiandroid-1047", "global", "default_catalog", "default_event_store")
  val filter = """eventTime<"2030-04-23T18:25:43.511Z""""

  val catalogClient = CatalogServiceClient.create(
    CatalogServiceSettings.newBuilder()
      .setCredentialsProvider { key }
      .build()
  )
  val userEventClient = UserEventServiceClient.create(
    UserEventServiceSettings.newBuilder()
      .setCredentialsProvider { key }
      .build()
  )

  val channel = Channel<Int>(Int.MAX_VALUE)
  val rate = (12000 / 60) - 10
  val writeRateLimiter = RateLimiter.create(rate.toDouble())
  val sendRateLimiter = RateLimiter.create(rate.toDouble())

//  val firstItem = args[1]
//  val fileName = "../${args[0]}"
//  println("fileName: $fileName firstItem: $firstItem")
//
//  val ids = File(fileName).readLines()
//  var sendCount = 0
//  launch {
//    for (id in ids) {
//      if (id > firstItem) {
//        val actualInt = id.toIntOrNull()
//        if (actualInt != null) {
//          channel.send(actualInt)
//        }
//      }
//    }
//  }

  launch(Dispatchers.IO) {
    catalogClient.listCatalogItems(catalogName, filter).iterateAll().forEach {
      it.id.toIntOrNull()?.let {
        sendRateLimiter.acquire()
        channel.send(it)
      }
    }
  }

  val deleted = AtomicInteger()
  val notFound = AtomicInteger()
  val startTime = System.currentTimeMillis()
  val operationCount = AtomicInteger()
  repeat(250) {
    launch(Dispatchers.IO) {
      for (item in channel) {
        val name = CatalogItemPathName
          .of("tikiandroid-1047", "global", "default_catalog", item.toString())
        try {
          writeRateLimiter.acquire()
          catalogClient.deleteCatalogItem(
            DeleteCatalogItemRequest.getDefaultInstance().toBuilder()
              .setName(name.toString())
              .build()
          )
          deleted.incrementAndGet()
        } catch (ex: Exception) {
          if (ex is NotFoundException) {
            notFound.incrementAndGet()
          } else {
            File("./error.txt").writeText(ex.message ?: "no error")
            throw ex
          }
        }
        operationCount.incrementAndGet()
        val endTime = System.currentTimeMillis()
        val totalDeleted = deleted.get()
        val totalNotFound = notFound.get()
        val speed = 1.0 * operationCount.get() / ((endTime - startTime) / 1000f)
        println("deleted ${totalDeleted} not found ${totalNotFound}, ${speed} items/s, item: $item")
        File("./process.txt").writeText("to item $item ${System.currentTimeMillis()}")
      }
    }
  }
}
/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package rai.script

import com.google.api.gax.rpc.NotFoundException
import com.google.auth.oauth2.ServiceAccountCredentials
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
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


val key = ServiceAccountCredentials.fromStream(
  System.getenv("TKPR_SA").let { Base64.getDecoder().decode(it) }.inputStream())

fun main(args: Array<String>): Unit = runBlocking {
  deleteCatalog(args)


}

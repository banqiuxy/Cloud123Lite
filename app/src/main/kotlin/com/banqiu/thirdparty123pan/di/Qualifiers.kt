package com.banqiu.thirdparty123pan.di

import javax.inject.Qualifier

/** 123pan 业务 API 客户端：携带授权和设备请求头。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiHttpClient

/** CDN 下载和 S3 上传客户端：不携带授权头，使用较大的连接池。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TransferHttpClient

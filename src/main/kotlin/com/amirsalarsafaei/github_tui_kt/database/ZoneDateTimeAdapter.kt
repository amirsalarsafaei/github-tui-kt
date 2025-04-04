package com.amirsalarsafaei.github_tui_kt.database

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ZoneDateTimeAdapter: app.cash.sqldelight.ColumnAdapter<ZonedDateTime, String>{
    override fun decode(databaseValue: String): ZonedDateTime {
        return ZonedDateTime.parse(databaseValue)
    }

    override fun encode(value: ZonedDateTime): String {
        return value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}
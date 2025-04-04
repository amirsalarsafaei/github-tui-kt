package com.amirsalarsafaei.github_tui_kt.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class ZonedDateTimeAdapter : TypeAdapter<ZonedDateTime?>() {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: ZonedDateTime?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(formatter.format(value))
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): ZonedDateTime? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        val dateString = `in`.nextString()
        return ZonedDateTime.parse(dateString, formatter)
    }
}
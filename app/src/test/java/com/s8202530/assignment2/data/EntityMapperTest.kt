package com.s8202530.assignment2.data

import com.google.gson.JsonParser
import com.s8202530.assignment2.data.model.EntityMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMapperTest {

    private fun json(text: String) = JsonParser.parseString(text).asJsonObject

    @Test
    fun descriptionIsSeparated_andOtherFieldsKeepTheirOrder() {
        val entity = EntityMapper.map(
            json("""{"artistName":"Monet","year":1872,"description":"Impressionist painter"}""")
        )

        assertEquals("Impressionist painter", entity.description)
        assertEquals(listOf("artistName", "year"), entity.fields.map { it.key })
        assertEquals("Monet", entity.title)
        assertEquals("1872", entity.summaryFields.single().value)
    }

    @Test
    fun labelsAreReadable() {
        assertEquals("Artist Name", EntityMapper.labelFor("artistName"))
        assertEquals("Date Of Birth", EntityMapper.labelFor("date_of_birth"))
        assertEquals("Year", EntityMapper.labelFor("year"))
    }

    @Test
    fun listsAndNullsAreHandled() {
        val entity = EntityMapper.map(json("""{"name":"Basil","uses":["Pesto","Salad"],"origin":null}"""))

        assertEquals("Pesto, Salad", entity.fields.first { it.key == "uses" }.value)
        assertEquals("", entity.fields.first { it.key == "origin" }.value)
        assertNull(entity.description)
    }
}

package com.impostor.data

import dev.gitlive.firebase.firestore.FieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlayerNameUploadDocumentTest {
    @Test
    fun `should encode only names and a server timestamp`() {
        // Given
        val names = listOf("Ala", "Bartek", "Celina")
        val document = PlayerNameUploadDocument(names, FieldValue.serverTimestamp)

        // When
        val descriptor = PlayerNameUploadDocument.serializer().descriptor
        val fields = (0 until descriptor.elementsCount).map(descriptor::getElementName)

        // Then
        assertEquals(listOf("names", "createdAt"), fields)
        assertEquals(names, document.names)
    }
}

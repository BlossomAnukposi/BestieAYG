package com.bayg.services

import org.junit.Assert.assertEquals
import org.junit.Test

class NoAsAMessageSanitizerTest {

    @Test
    fun `extractMessage returns message field from JSON`() {
        val body = """{"message":"Touch grass today"}"""
        assertEquals("Touch grass today", NoAsAMessageSanitizer.extractMessage(body))
    }

    @Test
    fun `extractMessage returns raw body for non-JSON`() {
        val body = "Plain text quote"
        assertEquals("Plain text quote", NoAsAMessageSanitizer.extractMessage(body))
    }

    @Test
    fun `extractMessage falls back when message field is empty`() {
        val body = """{"message":""}"""
        assertEquals(body, NoAsAMessageSanitizer.extractMessage(body))
    }

    @Test
    fun `sanitizeMessage strips script blocks and HTML tags`() {
        val input = "Hello <script>alert(1)</script> world"
        assertEquals("Hello world", NoAsAMessageSanitizer.sanitizeMessage(input))
    }

    @Test
    fun `sanitizeMessage removes javascript URI scheme`() {
        val input = "Click javascript:alert(1) now"
        assertEquals("Click alert(1) now", NoAsAMessageSanitizer.sanitizeMessage(input))
    }

    @Test
    fun `sanitizeMessage truncates long input`() {
        val input = "a".repeat(600)
        val result = NoAsAMessageSanitizer.sanitizeMessage(input)
        assertEquals(501, result.length)
        assertEquals(true, result.endsWith("…"))
    }

    @Test
    fun `sanitizeMessage collapses whitespace`() {
        val input = "Too   many\n\nlines"
        assertEquals("Too many lines", NoAsAMessageSanitizer.sanitizeMessage(input))
    }
}

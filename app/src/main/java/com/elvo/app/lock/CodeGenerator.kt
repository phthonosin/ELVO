package com.elvo.app.lock

import java.security.SecureRandom

object CodeGenerator {
    private const val ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%&*?"

    fun generate(length: Int = 12): String {
        val rng = SecureRandom()
        return buildString { repeat(length) { append(ALPHABET[rng.nextInt(ALPHABET.length)]) } }
    }
}

package de.muenchen.appcenter.signalo

import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import de.muenchen.appcenter.signalo.utils.Constants
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

object SnapshotSerializer : Serializer<SnapshotContainer> {
    override suspend fun readFrom(input: InputStream): SnapshotContainer {
        return try {
            val ciphertext = input.readBytes()
            // don't try to decrypt an empty value
            if (ciphertext.isEmpty()) {
                return defaultValue
            }
            val plaintext =
                CryptoProvider.aead.decrypt(ciphertext, Constants.ASSOCIATED_DATA.toByteArray())
            Json.decodeFromString(
                deserializer = SnapshotContainer.serializer(),
                string = plaintext.decodeToString()
            )
        } catch (e: GeneralSecurityException) {
            Timber.e(e, "Snapshot Read Failed: Decryption failed")
            defaultValue
        } catch (e: SerializationException) {
            Timber.e(e, "Snapshot Read Failed: SerializationException")
            defaultValue
        } catch (e: IOException) {
            Timber.e(e, "Snapshot Read Failed: IOException")
            defaultValue
        }
    }

    override suspend fun writeTo(t: SnapshotContainer, output: OutputStream) {
        val plaintext = Json.encodeToString(SnapshotContainer.serializer(), t).toByteArray()
        val ciphertext =
            CryptoProvider.aead.encrypt(plaintext, Constants.ASSOCIATED_DATA.toByteArray())
        output.write(ciphertext)
    }

    override val defaultValue: SnapshotContainer
        get() = SnapshotContainer()
}
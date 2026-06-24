package de.muenchen.appcenter.signalo

import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object SnapshotSerializer : Serializer<SnapshotContainer> {
    override suspend fun readFrom(input: InputStream): SnapshotContainer {
        return try {
            Json.decodeFromString(
                deserializer = SnapshotContainer.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            Timber.d("Snapshot Read Failed: SerializationException Erorr: " + e.printStackTrace())
            defaultValue
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.d("Snapshot Read Failed: IOException Erorr: " + e.printStackTrace())
            defaultValue
        }
    }

    override suspend fun writeTo(t: SnapshotContainer, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = SnapshotContainer.serializer(), value = t
            ).encodeToByteArray()
        )
    }

    override val defaultValue: SnapshotContainer
        get() = SnapshotContainer()
}
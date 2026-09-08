package de.muenchen.appcenter.signalo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeystore
import de.muenchen.appcenter.signalo.utils.Constants


@SuppressLint("StaticFieldLeak")
object CryptoProvider {

    private lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val aead: Aead by lazy {
        AeadConfig.register()
        if (!AndroidKeystore.hasKey(Constants.MASTER_KEY_ALIAS)) {
            AndroidKeystore.generateNewAes256GcmKey(Constants.MASTER_KEY_ALIAS)
        }
        val masterAEAD = AndroidKeystore.getAead(Constants.MASTER_KEY_ALIAS)
        val sharedPrefsFile =
            appContext.getSharedPreferences(Constants.KEYSET_PREFS_FILE, MODE_PRIVATE)
        val storedKeySet64 = sharedPrefsFile.getString(Constants.KEYSET_NAME, null)
        val keyHandle = if (!storedKeySet64.isNullOrEmpty()) {
            //keyset is not Empty
            TinkProtoKeysetFormat.parseEncryptedKeyset(
                Base64.decode(storedKeySet64, Base64.NO_WRAP),
                masterAEAD,
                Constants.ASSOCIATED_DATA.toByteArray(), RegistryConfiguration.get()
            )
        } else {
            // if keyset is not present
            val newKeysetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
            val encryptedKeyset = TinkProtoKeysetFormat.serializeEncryptedKeyset(
                newKeysetHandle,
                masterAEAD,
                Constants.ASSOCIATED_DATA.toByteArray(), RegistryConfiguration.get()
            )
            val newKeyset64 = Base64.encodeToString(encryptedKeyset, Base64.NO_WRAP)
            sharedPrefsFile.edit().putString(
                Constants.KEYSET_NAME,
                newKeyset64
            ).commit()
            // TODO: catch failed commit
            newKeysetHandle
        }
        keyHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
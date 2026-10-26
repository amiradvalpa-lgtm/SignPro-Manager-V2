package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "signage_app_prefs")

data class ContractorInfo(
    val name: String = "امیرحسین حسنخانی",
    val businessName: String = "تابلوسازی تبلیغاتی آلپا",
    val address: String = "فردوسی ۲، نبش خیابان فاضل",
    val phone: String = "09144862950",
    val accountNumber: String = "0115396517004",
    val shabaNumber: String = "IR250170000000115396517004",
    val cardNumber: String = "6037998209583854",
    val bankName: String = "بانک ملی ایران - امیرحسین حسنخانی"
)

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_CURRENCY = stringPreferencesKey("currency_unit") // "TOMAN" or "RIAL"
        val KEY_THEME = stringPreferencesKey("app_theme") // "SYSTEM", "LIGHT", "DARK"
        val KEY_COLOR = stringPreferencesKey("primary_color") // "AMBER", "BLUE", "CYAN", etc.
        val KEY_BACKUP_PATH = stringPreferencesKey("backup_folder_path")

        // Contractor keys
        val KEY_CONTRACTOR_NAME = stringPreferencesKey("contractor_name")
        val KEY_CONTRACTOR_BUSINESS = stringPreferencesKey("contractor_business")
        val KEY_CONTRACTOR_ADDRESS = stringPreferencesKey("contractor_address")
        val KEY_CONTRACTOR_PHONE = stringPreferencesKey("contractor_phone")
        val KEY_CONTRACTOR_ACCOUNT = stringPreferencesKey("contractor_account")
        val KEY_CONTRACTOR_SHABA = stringPreferencesKey("contractor_shaba")
        val KEY_CONTRACTOR_CARD = stringPreferencesKey("contractor_card")
        val KEY_CONTRACTOR_BANK = stringPreferencesKey("contractor_bank")

        // Auth
        val KEY_AUTH_ENABLED = booleanPreferencesKey("auth_enabled")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_WARN_UNSAVED = booleanPreferencesKey("warn_unsaved_changes")
    }

    val warnUnsavedChanges: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WARN_UNSAVED] ?: true
    }

    suspend fun setWarnUnsavedChanges(warn: Boolean) {
        context.dataStore.edit { it[KEY_WARN_UNSAVED] = warn }
    }

    val currencyUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: "TOMAN"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    val primaryColorKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_COLOR] ?: "AMBER"
    }

    val backupFolderPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKUP_PATH] ?: ""
    }

    val isAuthEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_ENABLED] ?: false
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val contractorInfo: Flow<ContractorInfo> = context.dataStore.data.map { prefs ->
        ContractorInfo(
            name = prefs[KEY_CONTRACTOR_NAME] ?: "امیرحسین حسنخانی",
            businessName = prefs[KEY_CONTRACTOR_BUSINESS] ?: "تابلوسازی تبلیغاتی آلپا",
            address = prefs[KEY_CONTRACTOR_ADDRESS] ?: "فردوسی ۲، نبش خیابان فاضل",
            phone = prefs[KEY_CONTRACTOR_PHONE] ?: "09144862950",
            accountNumber = prefs[KEY_CONTRACTOR_ACCOUNT] ?: "0115396517004",
            shabaNumber = prefs[KEY_CONTRACTOR_SHABA] ?: "IR250170000000115396517004",
            cardNumber = prefs[KEY_CONTRACTOR_CARD] ?: "6037998209583854",
            bankName = prefs[KEY_CONTRACTOR_BANK] ?: "بانک ملی ایران - امیرحسین حسنخانی"
        )
    }

    suspend fun setCurrencyUnit(unit: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = unit }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setPrimaryColorKey(colorKey: String) {
        context.dataStore.edit { it[KEY_COLOR] = colorKey }
    }

    suspend fun setBackupFolderPath(path: String) {
        context.dataStore.edit { it[KEY_BACKUP_PATH] = path }
    }

    suspend fun setAuthEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTH_ENABLED] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun updateContractorInfo(info: ContractorInfo) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTRACTOR_NAME] = info.name
            prefs[KEY_CONTRACTOR_BUSINESS] = info.businessName
            prefs[KEY_CONTRACTOR_ADDRESS] = info.address
            prefs[KEY_CONTRACTOR_PHONE] = info.phone
            prefs[KEY_CONTRACTOR_ACCOUNT] = info.accountNumber
            prefs[KEY_CONTRACTOR_SHABA] = info.shabaNumber
            prefs[KEY_CONTRACTOR_CARD] = info.cardNumber
            prefs[KEY_CONTRACTOR_BANK] = info.bankName
        }
    }
}

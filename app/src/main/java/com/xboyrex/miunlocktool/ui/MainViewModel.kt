package com.xboyrex.miunlocktool.ui

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xboyrex.miunlocktool.data.AccountStatus
import com.xboyrex.miunlocktool.data.ApplyResult
import com.xboyrex.miunlocktool.data.CheckStatusResult
import com.xboyrex.miunlocktool.data.ExecutionMode
import com.xboyrex.miunlocktool.data.LogColorType
import com.xboyrex.miunlocktool.data.LogEntry
import com.xboyrex.miunlocktool.data.PingTester
import com.xboyrex.miunlocktool.data.PreferencesManager
import com.xboyrex.miunlocktool.data.SntpClient
import com.xboyrex.miunlocktool.data.TranslationManager
import com.xboyrex.miunlocktool.data.XiaomiApiService
import com.xboyrex.miunlocktool.data.XiaomiSessionCredentials
import com.xboyrex.miunlocktool.fastboot.FastbootConnectionState
import com.xboyrex.miunlocktool.fastboot.FastbootManager
import com.xboyrex.miunlocktool.fastboot.FastbootUsbTransport
import com.xboyrex.miunlocktool.service.ApplyScheduler
import com.xboyrex.miunlocktool.service.LiveNotificationManager
import com.xboyrex.miunlocktool.unlock.MiUnlockCrypto
import com.xboyrex.miunlocktool.unlock.MiUnlockWorkflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PreferencesManager(application)
    val apiService = XiaomiApiService()
    val sntpClient = SntpClient()
    val pingTester = PingTester()
    val notificationManager = LiveNotificationManager(application, prefs)

    val scheduler = ApplyScheduler(
        prefs = prefs,
        apiService = apiService,
        sntpClient = sntpClient,
        pingTester = pingTester,
        scope = viewModelScope,
        notificationManager = notificationManager,
        context = application
    )

    private val _token = MutableStateFlow(prefs.token)
    val token: StateFlow<String> = _token.asStateFlow()

    private val _userId = MutableStateFlow(prefs.userId)
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _ssecurity = MutableStateFlow(prefs.ssecurity)
    val ssecurity: StateFlow<String> = _ssecurity.asStateFlow()

    private val _passToken = MutableStateFlow(prefs.passToken)
    val passToken: StateFlow<String> = _passToken.asStateFlow()

    private val _deviceId = MutableStateFlow(prefs.deviceId.ifEmpty { apiService.generateDeviceId() })
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _unlockZone = MutableStateFlow(prefs.unlockZone)
    val unlockZone: StateFlow<String> = _unlockZone.asStateFlow()

    private val _accountStatus = MutableStateFlow<CheckStatusResult?>(null)
    val accountStatus: StateFlow<CheckStatusResult?> = _accountStatus.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(false)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()

    private val _lastApplyDialogResult = MutableStateFlow<ApplyResult?>(null)
    val lastApplyDialogResult: StateFlow<ApplyResult?> = _lastApplyDialogResult.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _executionMode = MutableStateFlow(
        if (prefs.executionMode == ExecutionMode.MANUAL.id) ExecutionMode.MANUAL else ExecutionMode.AUTO
    )
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    private val _timingProfile = MutableStateFlow(prefs.timingProfile)
    val timingProfile: StateFlow<String> = _timingProfile.asStateFlow()

    private val _customTimings = MutableStateFlow(prefs.customTimings)
    val customTimings: StateFlow<String> = _customTimings.asStateFlow()

    private val _ntpServer = MutableStateFlow(prefs.ntpServer)
    val ntpServer: StateFlow<String> = _ntpServer.asStateFlow()

    private val _burstCount = MutableStateFlow(prefs.burstCount)
    val burstCount: StateFlow<Int> = _burstCount.asStateFlow()

    private val _burstIntervalMs = MutableStateFlow(prefs.burstIntervalMs)
    val burstIntervalMs: StateFlow<Long> = _burstIntervalMs.asStateFlow()

    private val _hyperIslandEnabled = MutableStateFlow(prefs.hyperIslandEnabled)
    val hyperIslandEnabled: StateFlow<Boolean> = _hyperIslandEnabled.asStateFlow()

    private val _autoNtpEnabled = MutableStateFlow(prefs.autoNtpEnabled)
    val autoNtpEnabled: StateFlow<Boolean> = _autoNtpEnabled.asStateFlow()

    private val _autoPingEnabled = MutableStateFlow(prefs.autoPingEnabled)
    val autoPingEnabled: StateFlow<Boolean> = _autoPingEnabled.asStateFlow()

    // Fastboot USB OTG & MiUnlockWorkflow Layer
    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    val fastbootTransport = FastbootUsbTransport(usbManager)
    val fastbootManager = FastbootManager(fastbootTransport)
    val miUnlockWorkflow = MiUnlockWorkflow(fastbootManager)

    private val _fastbootDeviceState = MutableStateFlow(FastbootConnectionState.NO_DEVICE)
    val fastbootDeviceState: StateFlow<FastbootConnectionState> = _fastbootDeviceState.asStateFlow()

    private val _fastbootDeviceInfoText = MutableStateFlow("No USB Fastboot device detected")
    val fastbootDeviceInfoText: StateFlow<String> = _fastbootDeviceInfoText.asStateFlow()

    private val _fastbootProduct = MutableStateFlow<String?>(null)
    val fastbootProduct: StateFlow<String?> = _fastbootProduct.asStateFlow()

    private val _fastbootToken = MutableStateFlow<String?>(null)
    val fastbootToken: StateFlow<String?> = _fastbootToken.asStateFlow()

    private val _manualProduct = MutableStateFlow("")
    val manualProduct: StateFlow<String> = _manualProduct.asStateFlow()

    private val _manualDeviceToken = MutableStateFlow("")
    val manualDeviceToken: StateFlow<String> = _manualDeviceToken.asStateFlow()

    private val _encryptDataHex = MutableStateFlow<String?>(null)
    val encryptDataHex: StateFlow<String?> = _encryptDataHex.asStateFlow()

    private val _isRequestingEncryptData = MutableStateFlow(false)
    val isRequestingEncryptData: StateFlow<Boolean> = _isRequestingEncryptData.asStateFlow()

    private val _clearWarningNotice = MutableStateFlow<String?>(null)
    val clearWarningNotice: StateFlow<String?> = _clearWarningNotice.asStateFlow()

    init {
        viewModelScope.launch {
            scheduler.logFlow.collect { entry ->
                val current = _logs.value.toMutableList()
                current.add(entry)
                if (current.size > 500) {
                    current.removeAt(0)
                }
                _logs.value = current
            }
        }
        viewModelScope.launch {
            scheduler.applyResultFlow.collect { result ->
                _lastApplyDialogResult.value = result
            }
        }
        // Respect the user's automatic synchronization preferences on launch.
        if (prefs.autoNtpEnabled) syncNtp()
    }

    fun dismissApplyResultDialog() {
        _lastApplyDialogResult.value = null
    }

    fun updateToken(rawInput: String) {
        val trimmed = rawInput.trim()
        // Automatically extract new_bbs_serviceToken if user pasted a full Cookie string
        val extracted = if (trimmed.contains("new_bbs_serviceToken=")) {
            val part = trimmed.substringAfter("new_bbs_serviceToken=").substringBefore(";").trim()
            addLog(TranslationManager.tr("log_token_cookie_detected"), LogColorType.CYAN)
            part
        } else {
            trimmed
        }
        prefs.token = extracted
        _token.value = extracted
    }

    fun applySessionCredentials(creds: XiaomiSessionCredentials) {
        updateToken(creds.token)
        if (creds.userId.isNotEmpty()) updateUserId(creds.userId)
        if (creds.ssecurity.isNotEmpty()) updateSsecurity(creds.ssecurity)
        if (creds.passToken.isNotEmpty()) updatePassToken(creds.passToken)
        if (creds.deviceId.isNotEmpty()) updateDeviceId(creds.deviceId)
        val masked = if (creds.token.length > 14) "${creds.token.take(8)}...${creds.token.takeLast(6)}" else "***"
        addLog("Xiaomi Auth Complete: Token ($masked) extracted", LogColorType.GREEN)
        checkAccountStatus()
    }

    fun signOutSession() {
        prefs.token = ""
        prefs.userId = ""
        prefs.ssecurity = ""
        prefs.passToken = ""
        _token.value = ""
        _userId.value = ""
        _ssecurity.value = ""
        _passToken.value = ""
        _accountStatus.value = null
        addLog("Signed out and cleared Xiaomi session tokens.", LogColorType.YELLOW)
    }

    fun updateUserId(value: String) {
        prefs.userId = value.trim()
        _userId.value = value.trim()
    }

    fun updateSsecurity(value: String) {
        prefs.ssecurity = value.trim()
        _ssecurity.value = value.trim()
    }

    fun updatePassToken(value: String) {
        prefs.passToken = value.trim()
        _passToken.value = value.trim()
    }

    fun updateDeviceId(value: String) {
        prefs.deviceId = value.trim()
        _deviceId.value = value.trim()
    }

    fun updateUnlockZone(zone: String) {
        prefs.unlockZone = zone
        _unlockZone.value = zone
        addLog("[AHA-UNLOCK] Server zone switched to $zone", LogColorType.CYAN)
    }

    fun updateManualProduct(product: String) {
        _manualProduct.value = product.trim()
    }

    fun updateManualDeviceToken(devToken: String) {
        _manualDeviceToken.value = devToken.trim()
    }

    fun updateExecutionMode(mode: ExecutionMode) {
        prefs.executionMode = mode.id
        _executionMode.value = mode
    }

    fun updateTimingProfile(profile: String) {
        prefs.timingProfile = profile
        _timingProfile.value = profile
    }

    fun updateCustomTimings(timings: String) {
        prefs.customTimings = timings
        _customTimings.value = timings
    }

    fun updateNtpServer(server: String) {
        prefs.ntpServer = server
        _ntpServer.value = server
        syncNtp()
    }

    fun updateBurstCount(count: Int) {
        val c = count.coerceIn(5, 50)
        prefs.burstCount = c
        _burstCount.value = c
    }

    fun updateBurstIntervalMs(intervalMs: Long) {
        val ms = intervalMs.coerceIn(25L, 500L)
        prefs.burstIntervalMs = ms
        _burstIntervalMs.value = ms
    }

    fun updateHyperIslandEnabled(enabled: Boolean) {
        prefs.hyperIslandEnabled = enabled
        _hyperIslandEnabled.value = enabled
    }

    fun updateAutoNtpEnabled(enabled: Boolean) {
        prefs.autoNtpEnabled = enabled
        _autoNtpEnabled.value = enabled
    }

    fun updateAutoPingEnabled(enabled: Boolean) {
        prefs.autoPingEnabled = enabled
        _autoPingEnabled.value = enabled
    }

    fun checkAccountStatus() {
        val currentToken = _token.value.trim()
        if (currentToken.isEmpty()) {
            addLog(TranslationManager.tr("log_error_token_empty"), LogColorType.RED)
            return
        }
        _isCheckingStatus.value = true
        _accountStatus.value = CheckStatusResult(AccountStatus.CHECKING, TranslationManager.tr("status_checking"))
        addLog("Checking Xiaomi account unlock eligibility...", LogColorType.CYAN)

        viewModelScope.launch {
            val result = apiService.checkAccountStatus(currentToken)
            _accountStatus.value = result
            _isCheckingStatus.value = false
            val logColor = when (result.status) {
                AccountStatus.READY, AccountStatus.APPROVED -> LogColorType.GREEN
                AccountStatus.BLOCKED, AccountStatus.TOO_NEW -> LogColorType.YELLOW
                else -> LogColorType.RED
            }
            addLog("Status Check: ${result.message}", logColor)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun getAllLogsText(): String {
        return _logs.value.joinToString("\n") { "[${it.timestamp}] ${it.text}" }
    }

    fun addLog(message: String, color: LogColorType = LogColorType.DEFAULT) {
        scheduler.log(message, color)
    }

    fun toggleScheduler() {
        if (scheduler.isRunning.value) {
            stopScheduler()
        } else {
            startScheduler(_executionMode.value)
        }
    }

    fun startScheduler(mode: ExecutionMode) {
        try {
            scheduler.start(mode)
        } catch (t: Throwable) {
            addLog("Error starting scheduler: ${t.localizedMessage ?: t.message}", LogColorType.RED)
        }
    }

    fun stopScheduler() {
        try {
            scheduler.stop()
        } catch (t: Throwable) {
            addLog("Error stopping scheduler: ${t.localizedMessage ?: t.message}", LogColorType.RED)
        }
    }

    fun syncNtp() {
        scheduler.syncNtp()
    }

    fun measurePing() {
        scheduler.measurePing()
    }

    // Fastboot USB OTG Actions
    fun checkAndConnectFastboot(context: Context) {
        val devices = fastbootTransport.findFastbootDevices()
        if (devices.isEmpty()) {
            _fastbootDeviceState.value = FastbootConnectionState.NO_DEVICE
            _fastbootDeviceInfoText.value = "No USB Fastboot device detected"
            addLog("[USB] No USB devices detected. Connect a Xiaomi device in Fastboot mode via USB OTG cable.", LogColorType.YELLOW)
            return
        }

        val target = devices.firstOrNull { it.hasFastbootInterface } ?: devices.first()
        val dev = target.device
        val devDesc = "${target.name} (VID: 0x%04X, PID: 0x%04X)".format(target.vendorId, target.productId)

        _fastbootDeviceInfoText.value = devDesc
        addLog("[USB] Device detected: $devDesc", LogColorType.CYAN)

        if (!fastbootTransport.hasPermission(dev)) {
            _fastbootDeviceState.value = FastbootConnectionState.PERMISSION_REQUIRED
            addLog("[USB] Requesting USB OTG permission for ${target.name}...", LogColorType.DEFAULT)
            fastbootTransport.requestPermission(context, dev)
        } else {
            addLog("[USB] USB OTG permission granted", LogColorType.GREEN)
            connectFastbootDevice(dev)
        }
    }

    fun onUsbPermissionResult(granted: Boolean, device: UsbDevice?) {
        if (granted && device != null) {
            addLog("[USB] Permission granted", LogColorType.GREEN)
            connectFastbootDevice(device)
        } else {
            _fastbootDeviceState.value = FastbootConnectionState.ERROR
            _fastbootDeviceInfoText.value = "USB permission denied"
            addLog("[USB] USB permission denied", LogColorType.RED)
        }
    }

    fun connectFastbootDevice(device: UsbDevice) {
        _fastbootDeviceState.value = FastbootConnectionState.CONNECTING
        addLog("[FASTBOOT] Connecting to ${device.deviceName}...", LogColorType.DEFAULT)

        val res = fastbootTransport.connect(device)
        if (res.isSuccess) {
            _fastbootDeviceState.value = FastbootConnectionState.CONNECTED
            addLog("[FASTBOOT] Bulk IN/OUT interface claimed. Fastboot connected!", LogColorType.GREEN)
            fetchFastbootProduct()
            fetchFastbootToken()
        } else {
            _fastbootDeviceState.value = FastbootConnectionState.ERROR
            val msg = res.exceptionOrNull()?.message ?: "Connection failed"
            _fastbootDeviceInfoText.value = msg
            addLog("[FASTBOOT] Connection error: $msg", LogColorType.RED)
        }
    }

    fun disconnectFastboot() {
        fastbootTransport.disconnect()
        _fastbootDeviceState.value = FastbootConnectionState.DISCONNECTED
        _fastbootDeviceInfoText.value = "Disconnected"
        _fastbootProduct.value = null
        _fastbootToken.value = null
        addLog("[FASTBOOT] Disconnected", LogColorType.YELLOW)
    }

    fun fetchFastbootProduct() {
        if (!fastbootTransport.isConnected) {
            addLog("[FASTBOOT] Device is not connected. Tap 'Connect OTG' first.", LogColorType.YELLOW)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            addLog("[FASTBOOT] Sending command: getvar:product", LogColorType.DEFAULT)
            val res = fastbootManager.getProduct()
            if (res.isSuccess) {
                val prod = res.getOrThrow()
                _fastbootProduct.value = prod
                _manualProduct.value = prod
                addLog("[FASTBOOT] OKAY -> Product: $prod", LogColorType.GREEN)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown error"
                addLog("[FASTBOOT] Error reading product: $err", LogColorType.RED)
            }
        }
    }

    fun fetchFastbootToken() {
        if (!fastbootTransport.isConnected) {
            addLog("[FASTBOOT] Device is not connected. Tap 'Connect OTG' first.", LogColorType.YELLOW)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            addLog("[FASTBOOT] Requesting bootloader token (oem get_token / getvar:token)...", LogColorType.DEFAULT)
            val res = fastbootManager.getToken()
            if (res.isSuccess) {
                val tok = res.getOrThrow()
                _fastbootToken.value = tok
                _manualDeviceToken.value = tok
                addLog("[FASTBOOT] Token received (${tok.length} chars): ${tok.take(24)}...", LogColorType.GREEN)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown error"
                addLog("[FASTBOOT] Failed to retrieve token: $err", LogColorType.RED)
            }
        }
    }

    fun rebootFastbootDevice(mode: String = "") {
        if (!fastbootTransport.isConnected) {
            addLog("[FASTBOOT] Device is not connected.", LogColorType.YELLOW)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val label = if (mode.isEmpty()) "system" else mode
            addLog("[FASTBOOT] Sending reboot ($label)...", LogColorType.CYAN)
            val res = fastbootManager.reboot(mode)
            if (res.isSuccess) {
                addLog("[FASTBOOT] Reboot command sent ($label).", LogColorType.GREEN)
                disconnectFastboot()
            } else {
                addLog("[FASTBOOT] Reboot failed: ${res.exceptionOrNull()?.message}", LogColorType.RED)
            }
        }
    }

    // Xiaomi ahaUnlock encryptData(token) retrieval workflow
    fun requestEncryptDataToken() {
        val prod = _manualProduct.value.ifEmpty { _fastbootProduct.value.orEmpty() }.trim()
        val devToken = _manualDeviceToken.value.ifEmpty { _fastbootToken.value.orEmpty() }.trim()
        val ssec = _ssecurity.value.trim()
        val uid = _userId.value.trim()
        val devId = _deviceId.value.trim().ifEmpty { apiService.generateDeviceId() }
        val srvToken = _token.value.trim()
        val zone = _unlockZone.value

        if (prod.isEmpty() || devToken.isEmpty()) {
            addLog("[AHA-UNLOCK] Product codename and Fastboot Device Token are required!", LogColorType.RED)
            return
        }
        if (ssec.isEmpty()) {
            addLog("[AHA-UNLOCK] ssecurity key is required for AES-CBC + HMAC-SHA1 signing. Sign in or paste ssecurity.", LogColorType.RED)
            return
        }

        _isRequestingEncryptData.value = true
        _clearWarningNotice.value = null
        addLog("[AHA-UNLOCK] Starting encryptData(token) retrieval for product '$prod' on $zone server...", LogColorType.PURPLE)

        viewModelScope.launch(Dispatchers.IO) {
            val cookies = buildMap {
                if (uid.isNotEmpty()) put("userId", uid)
                if (srvToken.isNotEmpty()) {
                    put("serviceToken", srvToken)
                    put("new_bbs_serviceToken", srvToken)
                }
                if (devId.isNotEmpty()) put("deviceId", devId)
            }

            val res = miUnlockWorkflow.obtainServerUnlockToken(
                zone = zone,
                product = prod,
                deviceToken = devToken,
                ssecurity = ssec,
                deviceId = devId,
                userId = uid,
                cookies = cookies,
                onClearWarning = { cleanOrNot, notice ->
                    if (notice.isNotEmpty()) {
                        _clearWarningNotice.value = "cleanOrNot=$cleanOrNot · $notice"
                        addLog("[AHA-UNLOCK] Device clear check: $notice (cleanOrNot=$cleanOrNot)", LogColorType.CYAN)
                    }
                }
            )

            _isRequestingEncryptData.value = false
            if (res.isSuccess) {
                val hex = res.getOrThrow()
                _encryptDataHex.value = hex
                addLog("[AHA-UNLOCK] SUCCESS! Retrieved encryptData(token) (${hex.length / 2} bytes)", LogColorType.GREEN)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown server error"
                addLog("[AHA-UNLOCK] Failed: $err", LogColorType.RED)
            }
        }
    }

    fun saveEncryptDataToBin(context: Context, onSaved: (String) -> Unit) {
        val hex = _encryptDataHex.value ?: return
        val prod = _manualProduct.value.ifEmpty { _fastbootProduct.value ?: "xiaomi" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = MiUnlockCrypto.hexToBytes(hex)
                val dlDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MiUnlockTool"
                ).apply { mkdirs() }
                val outFile = File(dlDir, "token_${prod}_${System.currentTimeMillis() / 1000}.bin")
                outFile.writeBytes(bytes)
                addLog("[AHA-UNLOCK] Saved binary unlock token to ${outFile.absolutePath}", LogColorType.GREEN)
                withContext(Dispatchers.Main) {
                    onSaved("Saved to Downloads/MiUnlockTool/${outFile.name}")
                }
            }.onFailure { e ->
                addLog("[AHA-UNLOCK] Could not save token.bin: ${e.message}", LogColorType.RED)
            }
        }
    }

    fun stageAndOemUnlockViaOtg() {
        val hex = _encryptDataHex.value
        if (hex.isNullOrEmpty()) {
            addLog("[FASTBOOT] Generate or paste encryptData(token) hex first!", LogColorType.RED)
            return
        }
        if (!fastbootTransport.isConnected) {
            addLog("[FASTBOOT] Connect a Fastboot device via USB OTG first!", LogColorType.RED)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            addLog("[FASTBOOT] Staging ${hex.length / 2}-byte unlock payload and executing 'oem unlock'...", LogColorType.PURPLE)
            val res = miUnlockWorkflow.performFastbootUnlock(hex)
            if (res.isSuccess) {
                addLog("[FASTBOOT] BOOTLOADER UNLOCKED SUCCESSFULLY! (${res.getOrThrow()})", LogColorType.GREEN)
                fastbootManager.reboot("")
            } else {
                addLog("[FASTBOOT] Unlock failed: ${res.exceptionOrNull()?.message}", LogColorType.RED)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fastbootTransport.disconnect()
    }
}

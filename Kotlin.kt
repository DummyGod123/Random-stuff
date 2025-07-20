
class LogViewModel : ViewModel() {

    private val _logsLiveData = MutableLiveData<String>()
    val logsLiveData: LiveData<String> = _logsLiveData

    private var logProcess: Process? = null
    private var logReaderThread: Thread? = null
    private val maxChars = 1000
    private val tag = "mine" // change this to your log tag

    init {
        startReadingLogs()
    }

    private fun startReadingLogs() {
        logReaderThread = Thread {
            try {
                // Optional: Clear previous logs
                Runtime.getRuntime().exec("logcat -c")

                // Start reading logcat
                logProcess = Runtime.getRuntime().exec("logcat")
                val reader = BufferedReader(InputStreamReader(logProcess!!.inputStream))

                val logBuffer = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains(tag)) {
                        logBuffer.appendLine(line)

                        // Trim to first 1000 characters only
                        if (logBuffer.length > maxChars) {
                            logBuffer.setLength(maxChars)
                        }

                        _logsLiveData.postValue(logBuffer.toString())
                        break // stop after first 1000 chars
                    }
                }

                reader.close()
                logProcess?.destroy()

            } catch (e: IOException) {
                _logsLiveData.postValue("Error reading logs: ${e.message}")
            }
        }

        logReaderThread?.start()
    }

    override fun onCleared() {
        super.onCleared()
        logProcess?.destroy()
        logReaderThread?.interrupt()
    }
}





class LogFragment : Fragment() {

    private lateinit var logTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val logTag = "mine" // change to match your tag

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log, container, false)
        logTextView = view.findViewById(R.id.logTextView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startLogUpdater()
    }

    private fun getLogcatLogs(): String {
        val logBuilder = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.contains(logTag)) {
                    logBuilder.append(line).append("\n")
                }
                line = reader.readLine()
            }
        } catch (e: IOException) {
            logBuilder.append("Error reading logs: ${e.message}")
        }
        return logBuilder.toString()
    }

    private val logUpdater = object : Runnable {
        override fun run() {
            logTextView.text = getLogcatLogs()
            handler.postDelayed(this, 2000) // update every 2 seconds
        }
    }

    private fun startLogUpdater() {
        handler.post(logUpdater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(logUpdater)
    }
}

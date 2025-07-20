class LogFragment : Fragment() {

    private lateinit var logTextView: TextView
    private var logProcess: Process? = null
    private var logReaderThread: Thread? = null
    private val logTag = "mine"
    private val handler = Handler(Looper.getMainLooper())

    // StringBuilder limited to last 2000 chars
    private val logBuffer = StringBuilder()
    private val maxChars = 2000

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
        startLiveLogCapture()
    }

    private fun startLiveLogCapture() {
        try {
            Runtime.getRuntime().exec("logcat -c") // optional: clear old logs
            logProcess = Runtime.getRuntime().exec("logcat")
            val reader = BufferedReader(InputStreamReader(logProcess!!.inputStream))

            logReaderThread = Thread {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains(logTag)) {
                        appendToBuffer(line!!)
                        updateLogTextView()
                    }
                }
            }
            logReaderThread?.start()

        } catch (e: IOException) {
            logTextView.text = "Error reading logs: ${e.message}"
        }
    }

    private fun appendToBuffer(newLine: String) {
        synchronized(logBuffer) {
            logBuffer.appendLine(newLine)
            if (logBuffer.length > maxChars) {
                // Trim from the start
                val excess = logBuffer.length - maxChars
                logBuffer.delete(0, excess)
            }
        }
    }

    private fun updateLogTextView() {
        handler.post {
            synchronized(logBuffer) {
                logTextView.text = logBuffer.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLogCapture()
    }

    private fun stopLogCapture() {
        logProcess?.destroy()
        logReaderThread?.interrupt()
    }
}

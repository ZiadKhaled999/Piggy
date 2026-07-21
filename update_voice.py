import re

file_path = "app/src/main/java/com/oryno/piggy_ledger/ui/components/VoiceRecordButton.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('if (isCancelling) "Release to cancel" else "Slide up to pause/cancel"', 
    'if (isCancelling) androidx.compose.ui.res.stringResource(com.oryno.piggy_ledger.R.string.release_to_cancel) else androidx.compose.ui.res.stringResource(com.oryno.piggy_ledger.R.string.slide_up_to_pause)')

content = content.replace('if (state == VoiceRecordState.RECORDING) "Recording..." else "Hold anywhere here to record"', 
    'if (state == VoiceRecordState.RECORDING) androidx.compose.ui.res.stringResource(com.oryno.piggy_ledger.R.string.recording_state) else androidx.compose.ui.res.stringResource(com.oryno.piggy_ledger.R.string.hold_anywhere_to_record)')

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated VoiceRecordButton.kt")

val intent = Intent(Intent.ACTION_SENDTO)
intent.data = Uri.parse("mailto:")
intent.putExtra(Intent.EXTRA_SUBJECT, "Subject")
intent.putExtra(Intent.EXTRA_STREAM, uri)

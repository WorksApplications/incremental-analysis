File log = new File(basedir, 'build.log')
assert log.exists()

assert  log.text.contains('Successfully generated list of target classes for SpotBugs.')
assert  log.text.contains('Updated class: com.example.Main')
assert !log.text.contains('Updated class: com.example.Another')

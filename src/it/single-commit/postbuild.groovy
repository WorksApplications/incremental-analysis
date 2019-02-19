File log = new File(basedir, 'build.log')
assert log.exists()

assert  log.text.contains('No updated Java class found, static analysis will be skipped.')

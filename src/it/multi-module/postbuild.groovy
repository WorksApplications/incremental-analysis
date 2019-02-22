File log = new File(basedir, 'build.log')
assert log.exists()

assert log.text.contains('No updated Java class found, static analysis will be skipped.')
assert log.text.contains('Successfully generated list of target classes for SpotBugs.')


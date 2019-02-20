@GrabResolver(name = 'jcenter', root = 'http://jcenter.bintray.com/')
@Grab('org.ajoberstar.grgit:grgit-core:3.0.0')
import org.ajoberstar.grgit.Grgit

def git = Grgit.init(dir: basedir)

git.add(patterns: ['src/main/java/com/example/Another.java'])
git.commit(message: 'initial commit')
git.checkout(branch: 'feature-branch', createBranch: true)
git.add(patterns: ['.'])
git.commit(message: 'second commit')
git.close()

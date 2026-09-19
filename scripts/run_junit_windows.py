"""Run the compiled Gradle JUnit suite without Gradle's non-ASCII worker-launcher issue."""
from pathlib import Path
import subprocess
import zipfile
import os
ROOT = Path(__file__).resolve().parents[1]
paths = (ROOT / 'app/build/unit-test-classpath.txt').read_text(encoding='utf-8').splitlines()
urls = []
for value in paths:
 path = Path(value)
 if not path.exists():
  if path.suffix == '.jar': raise SystemExit(f'Missing compiled test dependency: {path}')
  continue  # Gradle includes optional empty generated-resource directories.
 urls.append(path.as_uri() + ('/' if path.is_dir() else ''))
# Manifest URLs avoid platform code-page parsing of Unicode argument files.
text = 'Class-Path: ' + ' '.join(urls)
lines = []
while text:
 lines.append(text[:70]); text = (' ' + text[70:]) if text[70:] else ''
manifest = 'Manifest-Version: 1.0\r\n' + '\r\n'.join(lines) + '\r\n\r\n'
jar = ROOT / 'app/build/junit-classpath.jar'
with zipfile.ZipFile(jar, 'w') as out:
 out.writestr('META-INF/MANIFEST.MF', manifest.encode('ascii'))
java = str(Path(os.environ['JAVA_HOME'])/'bin/java.exe') if os.environ.get('JAVA_HOME') else 'java'
result = subprocess.run([java, '-cp', str(jar), 'org.junit.runner.JUnitCore',
 'com.banqiu.thirdparty123pan.DownloadAndDockTest', 'com.banqiu.thirdparty123pan.DownloadEngineTest'],
 cwd=ROOT, text=True, encoding='utf-8', errors='replace', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
(ROOT / 'app/build/junit-direct-results.txt').write_text(result.stdout, encoding='utf-8')
print(result.stdout)
raise SystemExit(result.returncode)

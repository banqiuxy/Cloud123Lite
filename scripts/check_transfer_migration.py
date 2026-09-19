"""Run the checked-in 2->3 SQL against SQLite and compare it to Room's exported schema."""
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / 'app/src/main/kotlin/com/banqiu/thirdparty123pan/data/db/TransferMigrations.kt'
schema = json.loads((ROOT / 'app/schemas/com.banqiu.thirdparty123pan.data.db.AppDatabase/3.json').read_text())
db = sqlite3.connect(':memory:')
db.execute('''CREATE TABLE transfer_tasks (
 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type INTEGER NOT NULL, fileId INTEGER NOT NULL,
 localPath TEXT, remoteDirId INTEGER NOT NULL, name TEXT NOT NULL, size INTEGER NOT NULL,
 progress REAL NOT NULL, speed INTEGER NOT NULL, status INTEGER NOT NULL, createTime INTEGER NOT NULL,
 finishTime INTEGER, error TEXT, etag TEXT, s3KeyFlag TEXT, url TEXT)''')
for status in range(6):
 db.execute('INSERT INTO transfer_tasks VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
  (status+1, status % 2, 42, '/old/download/file', 0, 'old.bin', 100, 0.5, 5, status, 1234, None, None, 'etag', 'key', 'https://example.invalid'))
before = db.execute('SELECT * FROM transfer_tasks').fetchall()
text = source.read_text(encoding='utf-8-sig')
for statement in re.findall(r'db\.execSQL\("([^"\n]+)"\)', text):
 if '$it' in statement:
  columns = re.search(r'listOf\(([^)]+)\)', text).group(1)
  for column in re.findall(r'"([^"]+)"', columns): db.execute(statement.replace('$it', column))
 else: db.execute(statement)
rows = db.execute('SELECT * FROM transfer_tasks').fetchall()
assert [row[:16] for row in rows] == before, 'Migration changed legacy tasks'
expected = schema['database']['entities'][0]
actual = {row[1]: row for row in db.execute('PRAGMA table_info(transfer_tasks)')}
assert set(actual) == {field['columnName'] for field in expected['fields']}
for field in expected['fields']:
 column = actual[field['columnName']]
 assert column[2] == field['affinity'], field
 assert bool(column[3]) == field.get('notNull', False), field
 assert column[4] == field.get('defaultValue'), (column, field)
assert db.execute('SELECT DISTINCT storageKind, phase, ownsOutput FROM transfer_tasks').fetchall() == [('legacy', 'download', 0)]
assert db.execute('SELECT COUNT(*) FROM transfer_tasks WHERE status IN (0,1,2,3,4,5)').fetchone()[0] == 6
print('PASS: 2->3 SQL preserves all six legacy task states and matches exported Room schema')


# BufferPoolManager (LRU) Demo

Java buffer pool manager using an LRU replacer; simulates page fetch/unpin/flush and appends flushed pages to disk.txt.

Files
- BufferPoolManager.java: Page struct, mock DiskManager, LRUReplacer, manager, and a small main demo.
- disk.txt: Appended output of flushed pages (created at runtime if missing).

Prerequisites
- JDK 8+ available on PATH (javac, java).
- PowerShell (commands below assume Windows).

Run
```
cd /d C:\xampp\htdocs\Tutorials\SOS
javac BufferPoolManager.java
java BufferPoolManager
```
Prints hit/miss metrics and appends flushed pages to disk.txt in this folder.

What the demo does
- Uses a buffer pool of size 3.
- Fetches pages 1–4, evicting via LRU when full.
- Marks pages dirty when specified; flushAll() writes dirty frames to disk.txt.
- Prints cache hit/miss counters and hit rate.

Notes
- DiskManager.readPage() returns placeholder data; it does not read back from disk.txt (persistence mocked).
- LRU recency is updated on unpin; long-held pinned pages may be treated as old when unpinned.
- Eviction skips frames with pinCount > 0; unpin pages to make them eligible.

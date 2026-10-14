Backup checkpoint, 2026-10-08 06:40 UTC

Laptop source archive: ../source_backup/
Remote compute workspace on each device: ~/Downloads/hpc/hpc2
Benchmark jobs remain active on the RustDesk hosts. These CSVs are snapshots copied while jobs were running, not the complete results.

Snapshots:
- 20261008T0640Z_q1_raw.csv: header plus 11 completed Q1 data rows. Remote SHA-256: d5b051134139562eb6e3f44d6e6bf87d255650991e7bd1f978af3dfbe7e1cd66
- 20261008T0640Z_q3_raw.csv: header plus 11 completed Q3 data rows. Remote SHA-256: 94ccdd4c8b8415fe13e1fca0f4c8114e9f0eb7251d1bc953e4e728090ba8be64
- 20261008T0640Z_q2_raw.csv: header plus 11 completed Q2 data rows. Remote SHA-256: 2d142a4b9b12455a3e71b9ca6a6049e587570f5632c768739ba5effcec980dab
- Q4 had not started at this checkpoint; device 3 runs it after Q2.

Transfer check: all CSV headers and completed rows were compared against the remote text after normalizing Windows line endings and trailing newlines. Compiled .class files are not part of this laptop source archive.

Checkpoint 2026-10-08 06:41 UTC:
- Q1: 20 completed data rows; remote SHA-256 a08c9aef67a9801648d1c73194c91d36b4374990cd31322d922326ff82023318
- Q3: 20 completed data rows; remote SHA-256 90a95e9a5501c0580c455ee0dba0bbd4a65b959d0a9f1e703e4afed23c2313d0
- Q2: 20 completed data rows; remote SHA-256 57f9c1065ad40fd9849cf9a1c8093095fd0eb75cc70501b6fb877773b4da5ab2
- Q4: not started; runs after Q2 on device 3.

Checkpoint 2026-10-09 06:13 UTC:
- Q1: completed. The remote results/q1_raw.csv contains 540 unique verified rows (plus header), covering both implementations, both sizes, all workloads, all thread counts, and five repetitions. Every measured interval is approximately 10 seconds (min 10,000,022,852 ns; max 10,005,392,518 ns). Laptop copy: ../results/q1_raw.csv. Remote and local SHA-256: 85b863c7f869d2f5338a61fbd8e4a87e3147707a31c0cb519ab05899a8a4f0ed.
- Device 1 process check found no running HPC2 process; the benchmark log ends after the final Q1 run.
- Devices 2 and 3 still returned connected=false after password submission. Their job state and any newer Q2/Q3/Q4 results remain unknown; do not restart them until their current files/processes can be inspected.
Checkpoint 2026-10-08 06:44 UTC:
- Q1: 40 rows; SHA-256 1af9cc2c761afad75b4a61c6f7d60a26fb0c95a427fad5d679b22a0f3be03ce9
- Q3: 40 rows; SHA-256 aec8ee03a66c4970a3e406236791e35453f6b6668bc7433bc0ddd2e7ebd0c65e
- Q2: 40 rows; SHA-256 c635621ea06f991629fab1fe6dac00cc3d5a594123fdd5e5cc7134751d55e3f8
- Q4: not started.
Checkpoint 2026-10-08 07:02 UTC:
- Q1: 145 data rows; SHA-256 7a974352594f88e34ae4315248e7ecf24ed067ec0d97dda431ac9100f3c59b98
- Q3: 145 data rows; SHA-256 d710a71307ac262f68ddcd6577f74b04afe655f9f945ac34f7ce2a5779442c15
- Q2: 145 data rows; SHA-256 833fbf0b7a82c6885288ce0ac4ce843ddf4d7e8b21a7c7d01e107aa187c7493a
- Q4: not started.
Checkpoint 2026-10-08 07:19 UTC:
- Q1: 250 data rows; remote snapshot SHA-256 873cd165ecb10ceddc886232bf6cb0b126248cf4dfb351634cb83f7c9792f910
- Q3: 250 data rows; remote snapshot SHA-256 c7985902a9159fb9e51e28457bb7f0101fa719d694a04426904d69e2f199f1df
- Q2: 250 data rows; remote snapshot SHA-256 49b73c6c589e2396cefce3f5e688329f67413f1a70540093f8dbd47d89803d5a
- Q4: not started; device 3 runs it after Q2.
- Hashes are over remote snapshot bytes; exclude the local patch-added trailing newline when verifying.
Status recheck, 2026-10-08 12:00 UTC: previous benchmark PIDs were 100688 (Q1), 503698 (Q3), and 267615 (Q2 then Q4), last confirmed live at 41:33 elapsed with 250 rows in each CSV. At 12:00 UTC all three RustDesk sessions returned connected=false/no displays after a fresh authentication attempt. The jobs' terminal state is unknown; do not restart them until reconnecting and inspecting their logs/CSV files.
Checkpoint 2026-10-08 07:16 UTC:
- Q1: 232 data rows; remote snapshot SHA-256 9ea259db6b62e7036ff156e424d1836315258198bb77f3eadbe972f8283cd914
- Q3: 232 data rows; remote snapshot SHA-256 fd063fe7e6bd9a6b2f0609d3acec1831ea4c907eac5a4d283f2370e8fdde3d58
- Q2: 232 data rows; remote snapshot SHA-256 6cfbbd092cd75a10bf86624b90f568c6ea5ed67a6d0926b1d47aff86376b3c68
- Q4: not started; device 3 runs it after Q2.
- Hashes are over remote snapshot bytes; exclude the local patch-added trailing newline when verifying.
Checkpoint 2026-10-08 07:13 UTC:
- Q1: 214 data rows; remote snapshot SHA-256 2a345ab48ffa6d008bf8483cd73725e4e72d0aeb0cecbcd42f09075039c096c4
- Q3: 214 data rows; remote snapshot SHA-256 227c49757dca0a801afdefa7cf01be2af8dd8e40b051370be7bc73a0c6d779ae
- Q2: 214 data rows; remote snapshot SHA-256 30496e603c33148bf1c41c8540677c56c5f8810be2421672ac9e39cffbf76e2c
- Q4: not started; device 3 runs it after Q2.
- The SHA-256 values are for remote snapshot bytes; the local patch-added final newline is excluded when verifying.
Checkpoint 2026-10-08 07:07 UTC:
- Q1: 172 data rows; remote snapshot SHA-256 f99ef7c41371a1496e494f2cc17edc36e1cd96b4c3e436555b0f1370e0484e4b
- Q3: 172 data rows; remote snapshot SHA-256 409824904e67d3f56c30d825eb1b9bef6cc6ac3dd642c993617ef5c4d731505e
- Q2: 172 data rows; remote snapshot SHA-256 343ccf6ea7a28f2d6c529a8a1b4890718ed8d6c8ab2981b4f34f6f4522240b6c
- Q4: not started; device 3 runs it after Q2.
- The SHA-256 values are for the remote snapshot bytes; the local patch-added final newline is excluded when verifying.
Checkpoint 2026-10-08 07:01 UTC (second checkpoint):
- Q1: 141 data rows; SHA-256 32d79bf685d7461dbf12ad027b7cec660a6a8728c64beea29ed3aec5e149ac00
- Q3: 141 data rows; SHA-256 52fceee38956cd6bbe0a18e9f2e3185faf2cb32f421dc00ca502ace07c52385d
- Q2: 141 data rows; SHA-256 9678043ebe758c0b50d5088d616b8d24f7446fcfe3cb5b66acea0a77bf795d2f
- Q4: not started.
Checkpoint 2026-10-08 07:01 UTC:
- Q1: 136 data rows; SHA-256 cc35a3992d7911d420a7f2bb1e65a43883e87e86777566ee27d7ca5d171370f1
- Q3: 136 data rows; SHA-256 7a9062fbb0dd208fc9fbae8eced3f9adaeff2b8196ff05c6ec31faf9822f244e
- Q2: 136 data rows; SHA-256 543a6392a5490c3fc03c54fa62073f359f39171a5eeb145e4c69be9aef1e8e7a
- Q4: not started.
Checkpoint 2026-10-08 07:00 UTC:
- Q1: 133 data rows; SHA-256 d2a2012869b8d03e4d3639cc45a0dec7fb4f21711ad89f925b18e77a7523986b
- Q3: 133 data rows; SHA-256 e497f3b5e54d743fabfa055d37b5ddb7b2f33268cf9c9f8acec19086710467de
- Q2: 133 data rows; SHA-256 7e1a6f54fb84788fa99502ceb1c3da1ea001fe8914acd063c77edf58e138caf3
- Q4: not started.
Checkpoint 2026-10-08 06:59 UTC:
- Q1: 129 data rows; SHA-256 49855d1461f8db4b02c8a1e05ecbf136b0e4df18b156140f3b4ffc49cc641223
- Q3: 129 data rows; SHA-256 f28a86fb7d6b3f6ffc6e813b6d054db1922a4670cc129da3b0c1c5a2a0c755da
- Q2: 129 data rows; SHA-256 bbd5f851f3413b134e86b402da98bb99ebba5a9fe754a19abf8370b23dd88929
- Q4: not started.
Checkpoint 2026-10-08 06:58 UTC:
- Q1: 121 data rows; SHA-256 162c1a983c14e385aacaee47f4463d2b19656c1b509308bb391d9986348d2d28
- Q3: 121 data rows; SHA-256 8b6f314e70bb79b785bbfe5fbb0acdc53bb287ae63f98216667dcddfc09886c6
- Q2: 121 data rows; SHA-256 e75d6b69e9ac03ac815bae79a654734def0f8a87573be016e11be0bcb70990ca
- Q4: not started.
Checkpoint 2026-10-08 06:57 UTC:
- Q1: 117 data rows; SHA-256 2d30b07bb2421cb2083ca6e5156c2ca59442b4b790c221ce09d75460b3d932ab
- Q3: 117 data rows; SHA-256 c9b4051446649b2763124cdcb7c2f7bee5fcce0b382dc07676f2cba5c2dae074
- Q2: 117 data rows; SHA-256 ced9a32eb89b93767cb66339e67b360082d0be006dece0178f68747e023f84f5
- Q4: not started.
Checkpoint 2026-10-08 06:55 UTC:
- Q1: 109 data rows; SHA-256 1475c62612639a49644bb2ee475789e10707ddea20f012802cd2204f898e873d
- Q3: 109 data rows; SHA-256 e4657af674ae61cee98ebb35c6d395d5ea8f80ffb0ab8e2be2f4ff71a7bac42f
- Q2: 109 data rows; SHA-256 5e1cefc9a44fe01eea1d232bc2f69e0626b18cbfc43b3d4d24e358af38396e6d
- Q4: not started.
Checkpoint 2026-10-08 06:54 UTC:
- Q1: 104 data rows; SHA-256 137021b8c08bf32558e229d18375a949a94f2823ea725fb1ee298178f5f15d0e
- Q3: 104 data rows; SHA-256 fd65eb30a38f17308fd64bbb46513e38bbdac9521951c10230f0ee766627365a
- Q2: 104 data rows; SHA-256 127f86e85ae7900fa393116206ae594db1cc8440c9ffec5814df28b98d2b2112
- Q4: not started.
Checkpoint 2026-10-08 06:53 UTC:
- Q1: 91 data rows; SHA-256 39f1aaea3c0c3d304b3e3d9a2cc3c5f2e5684ec4f680fb3ee643c2913af43426
- Q3: 91 data rows; SHA-256 060435ed5dc53bcb112fd3757f2064a4013e8ec5bc08868ed2d3c17e79bf6c20
- Q2: 91 data rows; SHA-256 271a3b04d775a8618e93dac21531b6c4fdc2add03c9c9b52a20eea2178786411
- Q4: not started.
Checkpoint 2026-10-08 06:52 UTC:
- Q1: 86 data rows; SHA-256 6e0a52e39a097382bfc1cf810a376a87226424693c860ad9d40ffe09c470af4f
- Q3: 86 data rows; SHA-256 c036ed2b3f80ac12c14594f87affc44b1a9f1b18802e3f686fb1c7d35679b8fc
- Q2: 86 data rows; SHA-256 cb673482c955a348fe2e3a501b5646af0bb73c81f860d155bd367dd0c696c5c7
- Q4: not started.
Checkpoint 2026-10-08 06:49 UTC:
- Q1: 74 data rows; SHA-256 4acfd7b071a010a781320b0b0780e85c5db03aec317d0cc40d08dd96a0c8964a
- Q3: 74 data rows; SHA-256 225a3314c8a44702b229dd63412a300f234a1437f446f1c37217d707153fcab9
- Q2: 74 data rows; SHA-256 0eb010d19007173f69706c7045751bead9084bd6745f97e2e77d6d2c73764cd7
- Q4: not started.
Checkpoint 2026-10-08 06:48 UTC:
- Q1: 67 data rows; SHA-256 fad8d6c901045bad95561c2e7b5c69eefa6e222acd094b776e5982fc2460c35f
- Q3: 67 data rows; SHA-256 0e02ee3b87aa5524a5e7c5248dd61e97ff9d54596dc6e98b561dbd7b746f8412
- Q2: 67 data rows; SHA-256 52bc084737274da8708cf42d54a95c9bcba1f99dd9567ea366638572ff605de3
- Q4: not started.
Checkpoint 2026-10-08 06:47 UTC:
- Q1: 60 data rows; SHA-256 3c1d1bf299138036a6d3aa9c7f8c80bafe7dc869328ca4ec0a8623c24e5aa98d
- Q3: 60 data rows; SHA-256 51073fd522b0b7f872be8992ad18856d2a3837dcfb1e2ab964c40c46c945cb25
- Q2: 60 data rows; SHA-256 4610413e04eef927ed695b5480e7f8cb52eb5cfabf763aa889790e19bedd84c2
- Q4: not started.
Checkpoint 2026-10-08 06:45 UTC:
- Q1: 49 data rows; remote SHA-256 ce121e3d31ee98ceaf398b7260dbd07d53810668c192ef884309bf454f6a7630
- Q3: 49 data rows; remote SHA-256 18114aa7533e74acb8636a9ba6154afc2a798044af493cc7ea0d105e903f21f8
- Q2: 49 data rows; remote SHA-256 bf8b705e635832f62817c351503b9d78df68dd4daa2c04fe53b4727f3286c540
- Q4: not started.
The timestamped CSVs for this checkpoint are saved alongside the first snapshot above.

Checkpoint 2026-10-08 06:43 UTC:
- Q1: 35 completed data rows; remote SHA-256 294ace6e41a89e327b610f83966eead37806fb3857396f77ddb7509c33aac692
- Q3: 35 completed data rows; remote SHA-256 9695ec480a189b90c06800ee7f74f037a7de383a709cb6f4fa7cb78e5fd36b37
- Q2: 35 completed data rows; remote SHA-256 aadaeb01b61c5557c7367e65369205ae7128846db5652728f6e3bd40a96617ad
- Q4: not started; runs after Q2 on device 3.

Checkpoint 2026-10-09 06:26 UTC:
- Q2: complete, 540 unique verified runs. Laptop copy: ../results/q2_raw.csv. Remote/local SHA-256: f2ceb6809a13f840abdedce8ccdcfbdf76fde593e693232f5f260969e8df02ae. Elapsed range: 10,000,016,644 to 10,001,167,084 ns.
- Q4: complete, 540 unique verified runs. Laptop copy: ../results/q4_raw.csv. Remote/local SHA-256: 4a124f575dd57a4a94963fb6a48e8255507bcc6658e7220c2b865cf78509670a. Elapsed range: 10,000,016,061 to 10,002,192,609 ns.
- Device 3 process check found no running HPC2 process. Q2 and Q4 logs end after the final rows.
- Device 2 (Q3 host) still returned connected=false after password submission. Its running state and final results remain unknown; the older 250-row checkpoint is not authoritative for the current state. Do not restart until the host can be inspected.

Access/validation update 2026-10-09 06:49 UTC:
- Devices 1 and 3 are reachable through RustDesk terminal sessions. Their completed Q1/Q2/Q4 raw results are saved under ../results/ and hash-verified against the hosts.
- Device 2 desktop is reachable and shows RustDesk Ready with the expected ID, but its RustDesk terminal session remains disconnected after password submission. The Q3 job/result state has therefore not been inspected.
- The enhanced laptop CorrectnessTest.java passed on Devices 1 and 3 (exit code 0) in isolated validation subdirectories. It was compiled against the remote Structures.java; the implementation and harness source contents match the laptop copies after normalizing line endings.

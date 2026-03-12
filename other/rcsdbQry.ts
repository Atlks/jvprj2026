import rocksdb from 'rocksdb';

type KV = [string, string];

export class RocksDBUtils {

    /**
     * 范围查询
     */
    static async rangeQuery(
        dbPath: string,
        startKey: string,
        order: 'asc' | 'desc' = 'desc',
        limit: number = 10
    ): Promise<KV[]> {

        const db = rocksdb(dbPath);

        await new Promise<void>((resolve, reject) => {
            db.open({ createIfMissing: false }, (err: Error | null) => {
                if (err) reject(err);
                else resolve();
            });
        });

        try {
            if (order === 'asc') {
                return await this.qryRangeAsc(db, startKey, limit);
            } else {
                return await this.qryRangeDesc(db, startKey, limit);
            }
        } finally {
            db.close(() => {});
        }
    }

    /**
     * 倒序查询
     */
    private static qryRangeDesc(
        db: any,
        startKey: string,
        limit: number
    ): Promise<KV[]> {

        return new Promise((resolve, reject) => {
            const result: KV[] = [];
            const it = db.iterator({ reverse: true });

            let count = 0;

            // 定位起点
            if (startKey === '@last') {
                it.end(() => step());
            } else if (startKey === '@first') {
                it.end(() => resolve([]));
                return;
            } else {
                it.seek(startKey);
                step();
            }

            function step() {
                it.next((err: Error | null, key?: Buffer, value?: Buffer) => {
                    if (err) {
                        it.end(() => reject(err));
                        return;
                    }
                    if (!key || count >= limit) {
                        it.end(() => resolve(result));
                        return;
                    }

                    result.push([key.toString(), value!.toString()]);
                    count++;
                    step();
                });
            }
        });
    }

    /**
     * 正序查询
     */
    private static qryRangeAsc(
        db: any,
        startKey: string,
        limit: number
    ): Promise<KV[]> {

        return new Promise((resolve, reject) => {
            const result: KV[] = [];
            const it = db.iterator();

            let count = 0;

            if (startKey === '@last') {
                it.end(() => resolve([]));
                return;
            } else if (startKey === '@first') {
                it.seekToFirst();
            } else {
                it.seek(startKey);
            }

            function step() {
                it.next((err: Error | null, key?: Buffer, value?: Buffer) => {
                    if (err) {
                        it.end(() => reject(err));
                        return;
                    }
                    if (!key || count >= limit) {
                        it.end(() => resolve(result));
                        return;
                    }

                    result.push([key.toString(), value!.toString()]);
                    count++;
                    step();
                });
            }

            step();
        });
    }
}

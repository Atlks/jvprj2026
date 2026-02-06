import {RocksDBUtils} from "./rcsdbQry.ts";

(async () => {
    const dbPath = 'C:\\Users\\attil\\IdeaProjects\\jvprj2026\\rocksdb-data1770366064462';
    const startKey = '2019687788127064230';

    console.log('=== Desc ===');
    const desc = await RocksDBUtils.rangeQuery(dbPath, startKey, 'desc', 10);
    desc.forEach(([k, v]) => console.log(`${k} -> ${v}`));

    console.log('desc finish...');

    console.log('=== Asc ===');
    const asc = await RocksDBUtils.rangeQuery(dbPath, startKey, 'asc', 10);
    asc.forEach(([k, v]) => console.log(`${k} -> ${v}`));
})();

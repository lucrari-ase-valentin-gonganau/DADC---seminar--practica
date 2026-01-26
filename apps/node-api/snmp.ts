import { Router } from "express";
import snmp from "net-snmp";
import { dbMongo } from "./db.ts";

const router = Router();

router.get("/", async (req, res) => {
  const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : 10;
  const offset = req.query.offset
    ? parseInt(req.query.offset as string, 10)
    : 0;
  const search = req.query.search;

  // order by timestamp desc
  // get from smtp
  const data = await dbMongo.connection
    .collection("snmp_data")
    .find(search ? { name: { $regex: search, $options: "i" } } : {})
    .skip(offset)
    .limit(limit)
    .sort({ timestamp: -1 })
    .toArray();

  res.json({ data });
});

// set timeout interval
let flag = true;
let count = 0;

const isDisabled = process?.env?.SNMP_POLLING === "false";

if (isDisabled) {
  console.log(
    "SNMP polling is disabled via environment variable 'SNMP_POLLING'.",
  );
}

!isDisabled &&
  setInterval(() => {
    if (!flag) {
      /// take a break;
      count++;

      if (count > 5) {
        throw new Error("SNMP operation taking too long, aborting.");
      }
      return;
    }

    count = 0;
    flag = false;

    const ipAddress =
      process.env && process.env.SNMP_TARGET_IP
        ? process.env.SNMP_TARGET_IP.split(",").map((ip) => ip.trim())
        : ["localhost"];

    const community = process.env.SNMP_COMMUNITY || "public";

    for (const ip of ipAddress) {
      console.log("Polling SNMP device at IP:", ip);

      const session = snmp.createSession(ip, community);
      const basicOids = [
        "1.3.6.1.2.1.1.1.0", // OS
        "1.3.6.1.4.1.2021.10.1.3.1", // CPU
        "1.3.6.1.4.1.2021.4.6.0", // RAM free
      ];

      session.get(basicOids, (error, varbinds) => {
        if (error) {
          console.error("SNMP Get Error:", error);
        } else {
          if (varbinds === undefined) {
            console.error("SNMP Get Error: varbinds is undefined");
            session.close();
            flag = true;
            return;
          }

          for (let i = 0; i < varbinds.length; i++) {
            if (snmp.isVarbindError(varbinds[i])) {
              console.error(
                "SNMP Varbind Error:",
                snmp.varbindError(varbinds[i]),
              );
            } else {
              // OS,
              // CPU,
              // RAM free
              // save to mongodb
              dbMongo.connection.collection("snmp_data").insertOne({
                name:
                  i === 0
                    ? `${ip}:OS`
                    : i === 1
                      ? `${ip}:CPU`
                      : `${ip}:RAM free`,
                oid: varbinds[i].oid,
                value: varbinds?.[i]?.value?.toString() || "",
                timestamp: new Date(),
              });
              console.log(`${varbinds[i].oid} = ${varbinds[i].value}`);
            }
          }
        }
        session.close();
        flag = true;
      });

      session.walk(
        "1.3.6.1.4.1.2021.9.1", // OID de bază pentru disk-uri
        20,
        (varbinds) => {
          for (let i = 0; i < varbinds.length; i++) {
            if (snmp.isVarbindError(varbinds[i])) {
              console.error(
                "SNMP Disk Walk Error:",
                snmp.varbindError(varbinds[i]),
              );
            } else {
              const oid = varbinds[i].oid;
              const value = varbinds[i].value?.toString() || "";

              // Determinăm tipul de date pe baza OID-ului
              let metricType = "unknown";
              if (oid.includes(".2.")) metricType = "path";
              else if (oid.includes(".6.")) metricType = "total_kb";
              else if (oid.includes(".7.")) metricType = "available_kb";
              else if (oid.includes(".8.")) metricType = "used_kb";
              else if (oid.includes(".9.")) metricType = "percent_used";

              dbMongo.connection.collection("snmp_disk_data").insertOne({
                ip: ip,
                name: `${ip}:Disk:${metricType}`,
                oid: oid,
                metric_type: metricType,
                value: value,
                timestamp: new Date(),
              });

              console.log(`Disk ${oid} = ${value} (${metricType})`);
            }
          }
        },
        (error) => {
          if (error) {
            console.error("SNMP Walk Error:", error);
          } else {
            console.log("SNMP Walk completed for disk OIDs.");
          }
        },
      );
    }
  }, 10000);

export { router };

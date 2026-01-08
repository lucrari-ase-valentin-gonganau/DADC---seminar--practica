import express from "express";
import { router as routerSnmp } from "./snmp.ts";
import cors from "cors";
import { dbMysql } from "./db.ts";

const app = express();
const PORT = process.env.PORT || 8082;

app.use(express.json());

app.use(cors());
// add cors

app.get("/", (req, res) => {
  res.send("Hello from Node.js API!");
});

app.get("/image/:id", async (req, res) => {
  const imageId = req.params.id;

  const [rows] = await dbMysql.query(
    "SELECT image_zoomed, image_blured, type_image FROM images WHERE id = ?",
    [imageId]
  );

  if ((rows as any[]).length === 0) {
    res.status(404).send("Image not found");
    return;
  }

  return res.json((rows as any[])?.[0]);
});

app.get("/image/blob/:id", async (req, res) => {
  const imageId = req.params.id;
  const type = req.query.type || "zoomed";
  // get image from blob from mysql
  const [rows] = await dbMysql.query(
    "SELECT image_zoomed, image_blured, type_image FROM images WHERE id = ?",
    [imageId]
  );

  type rowType = {
    image_zoomed: Buffer;
    image_blured: Buffer;
    type_image: string;
  };

  if ((rows as rowType[]).length === 0) {
    res.status(404).send("Image not found");
    return;
  }

  let contentType = "image/jpeg";
  if ((rows as rowType[])?.[0]?.type_image == "png") {
    contentType = "image/png";
  } else if ((rows as rowType[])?.[0]?.type_image == "gif") {
    contentType = "image/gif";
  } else if ((rows as rowType[])?.[0]?.type_image == "bmp") {
    contentType = "image/bmp";
  }

  res.setHeader("Content-Type", contentType);

  if (type === "blured") {
    res.send((rows as rowType[])[0].image_blured);
    return;
  }

  res.send((rows as rowType[])[0].image_zoomed);
});

app.use("/snmp", routerSnmp);

const server = app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});

server.on("error", (err) => {
  console.error("An error occurred:", err);
});

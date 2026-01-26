/* eslint-disable @next/next/no-img-element */
import { ApiNode } from "@/api";
import { useEffect, useState } from "react";

const ShowImage = ({ id }: { id: number }) => {
  const [loading, setLoading] = useState(true);
  const [imageUrl, setImageUrl] = useState<string | null>(null);

  const loadAboutImage = async (idImage: number) => {
    setLoading(true);
    try {
      const response = await ApiNode().get(`/image/${idImage}`);

      console.log("Image info:", response.data);

      // Revoke old URLs before creating new ones
      if (imageUrl) URL.revokeObjectURL(imageUrl);

      // Convert buffer to Blob
      const mimeType = `image/${response.data?.type_image || "bmp"}`;

      if (response.data?.image_zoomed?.data) {
        const zoomedBuffer = new Uint8Array(response.data.image_zoomed.data);
        const zoomedBlob = new Blob([zoomedBuffer], { type: mimeType });
        const url = URL.createObjectURL(zoomedBlob);
        setImageUrl(url);
      }

      setLoading(false);
    } catch (error) {
      console.error("Error loading the image info:", error);
      setLoading(false);
    }
  };

  useEffect(() => {
    (async () => {
      await loadAboutImage(id);
    })();

    return () => {
      if (imageUrl) {
        URL.revokeObjectURL(imageUrl);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  return (
    <div className="content-center my-4">
      {loading && (
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      )}
      {!!imageUrl && (
        <>
          <h3 className="text-center my-4">Zoomed Image</h3>

          <div className="text-center mb-3">
            <img
              src={imageUrl}
              alt="Processed image zoomed"
              style={{ maxWidth: "100%", height: "auto", borderRadius: "8px" }}
            />
          </div>

          <div className="text-center mt-3">
            <a
              href={imageUrl}
              download={`zoomed-image-${id}.bmp`}
              className="btn btn-primary"
            >
              <i className="bi bi-download me-2"></i>
              Download the image processed
            </a>
          </div>
        </>
      )}
    </div>
  );
};

export default ShowImage;

"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { ApiJavalin } from "@/api";

const UploadForm = () => {
  const router = useRouter();

  const [originalFile, setOriginalFile] = useState<File | null>(null);
  const [zoom, setZoom] = useState<number>(1);
  const [error, setError] = useState<string | null>(null);

  const handleSelectFile = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      const file = event.target.files[0];

      // Reset previous state
      setError(null);

      setOriginalFile(null);

      // Validate file size (max 2GB)
      const maxSize = 2 * 1024 * 1024 * 1024; // 2GB in bytes
      if (file.size > maxSize) {
        setError(
          `File is too large. Maximum size is 2GB. Your file is ${(
            file.size /
            (1024 * 1024)
          ).toFixed(2)}MB.`,
        );
        return;
      }

      // Validate file type (only BMP format)
      const allowedTypes = ["image/bmp"];
      if (!allowedTypes.includes(file.type)) {
        setError(
          `Invalid file format. Please use BMP only. Your file is ${
            file.type || "unknown format"
          }.`,
        );
        return;
      }

      setOriginalFile(file);

      // Load image for preview
      const reader = new FileReader();

      reader.addEventListener("error", () => {
        setError("Failed to load image preview.");
      });
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = () => {
    if (!originalFile) {
      setError("Please select a file.");
      return;
    }

    if (zoom <= 0) {
      setError("Please enter a valid zoom value.");
      return;
    }

    if (zoom < 0.1 || zoom > 10) {
      setError("Zoom value must be between 0.1 and 10.");
      return;
    }

    // create form data to send to backend
    const form = new FormData();
    form.append("file", originalFile);
    form.append("zoom", String(zoom));

    ApiJavalin({
      headers: { "Content-Type": "multipart/form-data" },
    })
      .post("/upload-and-process", form)
      .then((response) => {
        router.push("/upload/result?uploadId=" + response.data);
      })
      .catch((error) => {
        alert("Error sending the image. Check the console for details.");
        console.error("Error sending the image:", error);
      });
  };

  return (
    <div className="container-fluid d-flex justify-content-center align-items-center min-vh-100">
      <div
        className="card shadow-lg"
        style={{ maxWidth: "600px", width: "100%" }}
      >
        <div className="card-header text-center bg-primary text-white">
          <h2 className="mb-0">Upload Image to be Processed</h2>
        </div>
        <div className="card-body p-4">
          <div className="mb-3">
            <label htmlFor="fileInput" className="form-label fw-bold">
              Select Image:
            </label>
            <input
              id="fileInput"
              type="file"
              accept="image/jpeg,image/jpg,image/png,image/bmp,image/gif"
              onChange={handleSelectFile}
              className="form-control"
            />
            <small className="text-muted">
              Max size: 2GB. Format: BMP only.
            </small>
          </div>

          {error && (
            <div className="alert alert-danger" role="alert">
              <i className="bi bi-exclamation-triangle me-2"></i>
              {error}
            </div>
          )}

          {originalFile && (
            <div className="text-center">
              <div className="alert alert-success mb-3" role="alert">
                <i className="bi bi-check-circle me-2"></i>
                <strong>File selected:</strong> {originalFile.name} (
                {(originalFile.size / (1024 * 1024)).toFixed(2)} MB)
              </div>

              <div className="mb-3">
                <label htmlFor="zoomInput" className="form-label fw-bold">
                  Zoom Level:
                </label>
                <div className="input-group">
                  <input
                    id="zoomInput"
                    type="number"
                    min="0.1"
                    max="10"
                    step="0.1"
                    value={zoom}
                    onChange={(e) => setZoom(Number(e.target.value))}
                    className="form-control"
                    placeholder="Enter zoom level (e.g., 1.0)"
                  />
                  <span className="input-group-text">x</span>
                </div>
                <small className="text-muted">
                  Enter a value between 0.1 and 10 (1.0 = original size)
                </small>
              </div>

              <button
                onClick={handleSubmit}
                className="btn btn-success btn-lg"
                disabled={zoom <= 0}
              >
                <i className="bi bi-send me-2"></i>
                Submit
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UploadForm;

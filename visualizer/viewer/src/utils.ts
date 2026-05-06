export async function http(
    request: RequestInfo
  ): Promise<any> {
    const response = await fetch(request);
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }
    const contentType = response.headers.get("content-type");
    if (!contentType || !contentType.includes("application/json")) {
      throw new Error(`Expected JSON but received: ${contentType}`);
    }
    const body = await response.json();
    return body;
  }
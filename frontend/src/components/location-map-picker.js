"use client";

import { useEffect, useRef } from "react";

export default function LocationMapPicker({ position, onPositionChange }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const accuracyCircleRef = useRef(null);
  const positionRef = useRef(position);
  const changeHandlerRef = useRef(onPositionChange);

  useEffect(() => {
    positionRef.current = position;
  }, [position]);

  useEffect(() => {
    changeHandlerRef.current = onPositionChange;
  }, [onPositionChange]);

  useEffect(() => {
    let disposed = false;

    async function initializeMap() {
      const L = await import("leaflet");
      if (disposed || !containerRef.current || mapRef.current) return;

      const current = positionRef.current;
      const map = L.map(containerRef.current, {
        zoomControl: true,
        attributionControl: true,
      }).setView(
        [current.latitude, current.longitude],
        current.zoom ?? (current.accuracy > 1000 ? 12 : 16),
      );

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors",
      }).addTo(map);

      const markerVisible = current.source !== "fallback";
      markerRef.current = L.circleMarker([current.latitude, current.longitude], {
        radius: markerVisible ? 9 : 0,
        color: "#ffffff",
        weight: 3,
        fillColor: "#1765f5",
        opacity: markerVisible ? 1 : 0,
        fillOpacity: markerVisible ? 1 : 0,
      }).addTo(map);

      accuracyCircleRef.current = L.circle([current.latitude, current.longitude], {
        radius: Math.max(0, current.accuracy || 0),
        color: "#1765f5",
        weight: 1,
        opacity: 0.5,
        fillColor: "#1765f5",
        fillOpacity: 0.08,
      }).addTo(map);

      map.on("click", ({ latlng }) => {
        changeHandlerRef.current({
          latitude: latlng.lat,
          longitude: latlng.lng,
          accuracy: 0,
          source: "manual",
        });
      });

      mapRef.current = map;
      window.setTimeout(() => map.invalidateSize(), 0);
    }

    initializeMap();
    return () => {
      disposed = true;
      mapRef.current?.remove();
      mapRef.current = null;
      markerRef.current = null;
      accuracyCircleRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !markerRef.current || !accuracyCircleRef.current) return;
    const latLng = [position.latitude, position.longitude];
    const markerVisible = position.source !== "fallback";
    markerRef.current
      .setLatLng(latLng)
      .setRadius(markerVisible ? 9 : 0)
      .setStyle({ opacity: markerVisible ? 1 : 0, fillOpacity: markerVisible ? 1 : 0 });
    accuracyCircleRef.current.setLatLng(latLng).setRadius(Math.max(0, position.accuracy || 0));
    mapRef.current.setView(latLng, position.zoom ?? (position.accuracy > 1000 ? 12 : 16));
  }, [position]);

  return <div ref={containerRef} className="location-map" aria-label="위치를 선택하는 지도" />;
}

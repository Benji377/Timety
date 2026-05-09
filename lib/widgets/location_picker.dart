import 'package:flutter/material.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:flutter/foundation.dart';
import '../theme/app_theme.dart';

class LocationPicker extends StatefulWidget {
  final String? initialLocation;

  const LocationPicker({super.key, this.initialLocation});

  @override
  State<LocationPicker> createState() => _LocationPickerState();
}

class _LocationPickerState extends State<LocationPicker> {
  bool _isLoading = true;
  bool _hasInternet = false;
  LatLng? _currentPosition;
  LatLng? _selectedPosition;
  final MapController _mapController = MapController();

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  // Helper to safely parse "Lat, Lng" strings back into map coordinates
  LatLng? _parseInitialLocation() {
    if (widget.initialLocation == null || widget.initialLocation!.isEmpty) {
      return null;
    }

    final parts = widget.initialLocation!.split(',');
    if (parts.length == 2) {
      final lat = double.tryParse(parts[0].trim());
      final lng = double.tryParse(parts[1].trim());
      if (lat != null && lng != null) {
        return LatLng(lat, lng);
      }
    }
    return null; // Returns null if it was a text address like "Webex Meeting"
  }

  Future<void> _initialize() async {
    final connectivityResult = await Connectivity().checkConnectivity();
    _hasInternet = !connectivityResult.contains(ConnectivityResult.none);

    // 1. Try to load the passed-in location first
    _selectedPosition = _parseInitialLocation();

    // 2. If no valid coordinates were passed, get the GPS location
    if (_selectedPosition != null) {
      _currentPosition = _selectedPosition;
    } else {
      await _determinePosition();
    }

    setState(() {
      _isLoading = false;
    });
  }

  Future<void> _determinePosition() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Location services are disabled on this device.'),
          ),
        );
      }
      return;
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Location permission was denied.')),
          );
        }
        return;
      }
    }
    if (permission == LocationPermission.deniedForever) {
      if (mounted) {
        final openSettings = await showDialog<bool>(
          context: context,
          builder: (dialogContext) => AlertDialog(
            title: const Text('Location Permission Required'),
            content: const Text(
              'Location access is permanently denied. Please enable it in system settings to use map location picking.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(false),
                child: const Text('Cancel'),
              ),
              ElevatedButton(
                onPressed: () => Navigator.of(dialogContext).pop(true),
                child: const Text('Open Settings'),
              ),
            ],
          ),
        );

        if (openSettings == true) {
          await Geolocator.openAppSettings();
        }
      }
      return;
    }

    LocationSettings locationSettings;

    if (defaultTargetPlatform == TargetPlatform.android) {
      locationSettings = AndroidSettings(
        accuracy: LocationAccuracy.medium,
        forceLocationManager:
            true, // Forces native Android GPS, bypassing Google Play Services
      );
    } else {
      locationSettings = const LocationSettings(
        accuracy: LocationAccuracy.medium,
      );
    }

    Position position = await Geolocator.getCurrentPosition(
      locationSettings: locationSettings,
    );
    _currentPosition = LatLng(position.latitude, position.longitude);

    // Only set selected position to current if we didn't pass one in
    _selectedPosition ??= _currentPosition;
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_hasInternet ? 'Pick Location' : 'Offline Mode'),
        actions: [
          if (_selectedPosition != null)
            IconButton(
              icon: const Icon(Icons.check),
              onPressed: () {
                Navigator.pop(
                  context,
                  "${_selectedPosition!.latitude.toStringAsFixed(5)}, ${_selectedPosition!.longitude.toStringAsFixed(5)}",
                );
              },
            ),
        ],
      ),
      body: _hasInternet ? _buildOnlineMap() : _buildOfflineFallback(),
      // Add a quick FAB so users can jump back to their physical GPS location
      floatingActionButton: _hasInternet
          ? FloatingActionButton(
              heroTag: "gps_button",
              onPressed: () async {
                await _determinePosition();
                if (_currentPosition != null) {
                  _mapController.move(_currentPosition!, 15.0);
                  setState(() => _selectedPosition = _currentPosition);
                }
              },
              child: const Icon(Icons.my_location),
            )
          : null,
    );
  }

  Widget _buildOnlineMap() {
    return FlutterMap(
      mapController: _mapController,
      options: MapOptions(
        initialCenter: _selectedPosition ?? const LatLng(0, 0),
        initialZoom: _selectedPosition == null ? 2.0 : 15.0,
        onTap: (tapPosition, point) {
          setState(() => _selectedPosition = point);
        },
      ),
      children: [
        TileLayer(
          urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
          userAgentPackageName: 'io.github.benji377.timety',
        ),
        // OFFICIAL OSM ATTRIBUTION
        const RichAttributionWidget(
          attributions: [TextSourceAttribution('© OpenStreetMap contributors')],
        ),
        if (_selectedPosition != null)
          MarkerLayer(
            markers: [
              Marker(
                point: _selectedPosition!,
                child: const Icon(
                  Icons.location_pin,
                  color: Colors.red,
                  size: 40,
                ),
              ),
            ],
          ),
      ],
    );
  }

  Widget _buildOfflineFallback() {
    return Center(
      child: Padding(
        padding: AppTheme.paddingScreenVertical,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.wifi_off, size: 64, color: Colors.grey),
            SizedBox(height: AppTheme.spaceLarge),
            const Text(
              'You are offline. The map cannot be displayed.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: AppTheme.fsBodyLarge),
            ),
            SizedBox(height: AppTheme.space2XLarge),
            if (_currentPosition != null) ...[
              const Text('We found your GPS coordinates:'),
              Text(
                "${_currentPosition!.latitude.toStringAsFixed(4)}, ${_currentPosition!.longitude.toStringAsFixed(4)}",
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                ),
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                icon: const Icon(Icons.my_location),
                label: const Text('Use These Coordinates'),
                onPressed: () {
                  Navigator.pop(
                    context,
                    "${_currentPosition!.latitude.toStringAsFixed(5)}, ${_currentPosition!.longitude.toStringAsFixed(5)}",
                  );
                },
              ),
            ] else
              const Text(
                'Could not retrieve GPS signal. Please type your location manually on the previous screen.',
              ),
          ],
        ),
      ),
    );
  }
}

import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/settings_provider.dart';
import '../../l10n/app_localizations.dart';

class LocationPicker extends StatefulWidget {
  const LocationPicker({super.key});

  @override
  State<LocationPicker> createState() => _LocationPickerState();
}

class _LocationPickerState extends State<LocationPicker> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _debounce;
  bool _isLoading = false;
  List<dynamic> _searchResults = [];

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();

    // Only trigger search if the user has typed at least 3 characters
    if (query.trim().length >= 3) {
      _debounce = Timer(const Duration(milliseconds: 600), () {
        _performSearch(query.trim());
      });
    } else {
      // Clear results if user deletes text below 3 characters
      setState(() {
        _searchResults = [];
        _isLoading = false;
      });
    }

    setState(() {});
  }

  Future<void> _performSearch(String query) async {
    setState(() => _isLoading = true);

    try {
      final endpoint = context.read<SettingsProvider>().locationApiEndpoint;
      final baseUrl = endpoint.endsWith('/') ? endpoint : '$endpoint/';
      final url = Uri.parse('$baseUrl?q=$query&limit=10');

      // Inject the required headers
      final response = await http.get(
        url,
        headers: {
          'User-Agent': 'timety/1.0 (io.github.benji377.timety)',
          'Accept': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(utf8.decode(response.bodyBytes));
        if (mounted) {
          setState(() {
            _searchResults = data['features'] ?? [];
          });
        }
      } else {
        debugPrint('API Error: ${response.statusCode} - ${response.body}');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                AppLocalizations.of(
                  context,
                )!.locationPickerServerError(response.statusCode),
              ),
            ),
          );
        }
      }
    } catch (e) {
      debugPrint('Search error: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              AppLocalizations.of(context)!.locationPickerNetworkError,
            ),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  String _buildDetailsString(Map<String, dynamic> p) {
    final List<String> parts = [];

    // Type (e.g., restaurant, cafe, supermarket)
    final type = p['osm_value'] as String?;
    if (type != null && type.isNotEmpty && type != 'yes') {
      parts.add(type[0].toUpperCase() + type.substring(1));
    }

    // Street & Number
    final street = p['street'] as String?;
    final housenumber = p['housenumber'] as String?;
    String streetInfo = '';
    if (street != null) streetInfo += street;
    if (street != null && housenumber != null) streetInfo += ' $housenumber';
    if (streetInfo.isNotEmpty) parts.add(streetInfo);

    // City / State / Postal Code
    final city = p['city'] ?? p['town'] ?? p['village'] ?? '';
    final state = p['state'] ?? '';
    final postcode = p['postcode'] ?? '';

    final List<String> locationParts = [];
    if (city.isNotEmpty) locationParts.add(city);
    if (state.isNotEmpty) locationParts.add(state);
    if (postcode.isNotEmpty) locationParts.add(postcode);

    if (locationParts.isNotEmpty) {
      parts.add(locationParts.join(', '));
    }

    // Join everything with a bullet point
    return parts.join(' • ');
  }

  /// Fallback name generator in case the location doesn't have a specific "name"
  String _getPrimaryName(Map<String, dynamic> p, AppLocalizations l10n) {
    if (p['name'] != null && p['name'].toString().isNotEmpty) {
      return p['name'];
    }
    // If there is no name (like a random house), use street and number as title
    final street = p['street'];
    final number = p['housenumber'];
    if (street != null) {
      return number != null ? '$street $number' : street;
    }
    // Final fallback
    return p['city'] ?? p['state'] ?? l10n.locationPickerUnknown;
  }

  @override
  Widget build(BuildContext context) {
    final queryLength = _searchController.text.trim().length;
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.locationPickerTitle)),
      body: Column(
        children: [
          // Search Bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              autofocus: true,
              decoration: InputDecoration(
                hintText: l10n.locationPickerHint,
                prefixIcon: const Icon(Icons.search),
                suffixIcon: queryLength > 0
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchController.clear();
                          _onSearchChanged('');
                        },
                      )
                    : null,
                border: const OutlineInputBorder(
                  borderRadius: AppTheme.brLarge,
                ),
              ),
            ),
          ),

          if (_isLoading) const LinearProgressIndicator(),

          // Main Content Area
          Expanded(child: _buildBodyContent(queryLength, l10n)),
        ],
      ),
    );
  }

  Widget _buildBodyContent(int queryLength, AppLocalizations l10n) {
    // User hasn't typed enough yet
    if (queryLength < 3) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.search, size: 64, color: Theme.of(context).dividerColor),
            const SizedBox(height: 16),
            Text(
              l10n.locationPickerStartTyping,
              style: const TextStyle(fontSize: 16, color: Colors.grey),
            ),
            const SizedBox(height: 8),
            Text(
              l10n.locationPickerMinChars,
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      );
    }

    // Searching finished but no results found
    if (!_isLoading && _searchResults.isEmpty) {
      return Center(
        child: Text(
          l10n.locationPickerNoResults(_searchController.text.trim()),
          style: const TextStyle(fontSize: 16, color: Colors.grey),
        ),
      );
    }

    // Display Results
    return ListView.separated(
      itemCount: _searchResults.length,
      separatorBuilder: (context, index) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final feature = _searchResults[index];
        final properties = feature['properties'] as Map<String, dynamic>;

        final titleName = _getPrimaryName(properties, l10n);
        final detailedSubtitle = _buildDetailsString(properties);

        return ListTile(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 4,
          ),
          leading: const CircleAvatar(
            backgroundColor: Colors.transparent,
            child: Icon(Icons.location_on_outlined, color: Colors.grey),
          ),
          title: Text(
            titleName,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          subtitle: detailedSubtitle.isNotEmpty
              ? Text(
                  detailedSubtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                )
              : null,
          onTap: () {
            // Unfocus keyboard before popping for smoother animation
            FocusScope.of(context).unfocus();

            // Return just the title name to save as a string
            Navigator.pop(context, titleName);
          },
        );
      },
    );
  }
}

class Category {
  final int? id;
  final String name;
  final String colorHex;
  final String iconName;

  Category({
    this.id,
    required this.name,
    required this.colorHex,
    required this.iconName,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'colorHex': colorHex,
      'iconName': iconName,
    };
  }

  factory Category.fromMap(Map<String, dynamic> map) {
    return Category(
      id: map['id'],
      name: map['name'],
      colorHex: map['colorHex'],
      iconName: map['iconName'],
    );
  }
}

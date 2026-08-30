String mediaUrl(String base, String? path) {
  if (path == null || path.isEmpty) return '';
  if (RegExp(r'^https?:\/\/', caseSensitive: false).hasMatch(path)) {
    return path;
  }
  final normalized = base.replaceFirst(RegExp(r'/+$'), '');
  return path.startsWith('/') ? '$normalized$path' : '$normalized/$path';
}

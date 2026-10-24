package com.oldschoolnetwork.store;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class CatalogFilterRegressionTest {
 private StoreRepository fixture() throws Exception {
  Constructor<StoreRepository> constructor = StoreRepository.class.getDeclaredConstructor(); constructor.setAccessible(true);
  StoreRepository repository = constructor.newInstance(); Field field = StoreRepository.class.getDeclaredField("catalogApps"); field.setAccessible(true);
  List<AppItem> apps = (List<AppItem>) field.get(repository);
  apps.add(AppItem.fromJson(new JSONObject("{\"id\":1,\"name\":\"Alpha Tool\",\"app_type\":\"app\",\"genres_list\":[\"Tools\"],\"downloads\":10}")));
  apps.add(AppItem.fromJson(new JSONObject("{\"id\":2,\"name\":\"Alpha Game\",\"app_type\":\"game\",\"genres_list\":[\"Arcade\"],\"downloads\":100}")));
  return repository;
 }
 @Test public void emptyGenreSelectionMeansNoResults() throws Exception {
  StoreRepository repository = fixture(); repository.getFilterState(false).selectAllGenres = false;
  assertTrue(repository.getCategorizedItems(false).isEmpty());
 }
 @Test public void genreListsDoNotLeakBetweenGamesAndApps() throws Exception {
  StoreRepository repository = fixture(); assertEquals(java.util.Arrays.asList("Tools"), repository.getGenresForType(false)); assertEquals(java.util.Arrays.asList("Arcade"), repository.getGenresForType(true));
 }
 @Test public void searchUsesBothAppTypesAndHasItsOwnFilter() throws Exception {
  StoreRepository repository = fixture(); repository.setSearchQuery("Alpha"); repository.getSearchFilter().sortBy = "downloads";
  List<Object> items = repository.getSearchItems(); assertEquals(3, items.size());
  assertEquals("Alpha Game", ((AppItem) items.get(1)).getTitle());
  repository.getSearchFilter().selectAllGenres = false; repository.getSearchFilter().selectedGenres.add("Tools");
  items = repository.getSearchItems(); assertEquals(2, items.size()); assertEquals("Alpha Tool", ((AppItem) items.get(1)).getTitle());
 }
}

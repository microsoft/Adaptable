# Adaptable
## What's it?
**Adaptable** is a highly conservative representation of long-living, often-modified, naturally ordered sequences of items. You can think of it as of a small in-memory cache or database. It fills the gap between `List` and `SortedSet` by allowing _O(log N)_ access **_both by item value and by numeric item index_**.

Multiple predicate-based selections of the stored data can be exposed and randomly accessed by element index, allowing value-to-index, index-to-value and cross index-to-index lookup.

Element-wise updates (unlike those in `SortedList`, which is array-backed) are _O(log N)_; further optimization is possible by allowing updates that knowingly leave item ordering and/or predicate satisfaction intact.

Thanks to probabilistic partitioning, the underlying storage never needs to be rebalanced. Update notifications are selection-wise and range-wise in a way expected by [RecyclerView.Adapter](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html).

Bonus features include

+ predicate chaining and custom dependencies between predicates (without cycles);
+ comparison order and selection predicate update on the fly;
+ optional support of duplicate items, as in a `Multiset`;
+ item grouping into ranges with automatically inserted range headers and cached range length (as in _"there are 3 clients with last names starting with **A**"_).

## Examples

Let's assume

    public interface Task {
        public int id(); // primary index for tie breaking
        public String name();
        public String description();
        public float importance();
        public Person assignee();
    }

Then the following ordering and filters can be defined (in Java 8 syntax):

    Comparator<Task> importantFirst = Comparator.comparing(Task.importance).reversed().thenComparing(Task.id);    
    Filter<Task> isWellDefined = (item) -> item.description().length() > item.name().length();
    Filter<Task> isBurning = (item) -> item.importance() >= Importance.BURNING.value();
    Filter<Task> isMine = (item) -> item.assignee().isMe();

Based on the ordering and filters, we can create an Adaptable task list:

    AdaptableFactory<Task> factory = new AdaptableFactory<>();
    factory.setComparator(importantFirst);
    final int definedFilterId = factory.addFilter(isWellDefined);
    final int burningFilterId = factory.addFilter(isBurning);
    final int myTasksFilterId = factory.addFilter(isMine);
    Adaptable<Task> adaptable = factory.create();
    final int allPassFilterId = adaptabe.getUniverseFilterId(); // 0
    
Then we can run the following operations on the list:

    adaptable.add(wakeUpTask); // will appear in universe and matching selections
    adaptable.add(wakeUpTask); // no-op, because we haven't allowed duplicates
    adaptable.updateInPlace(wakeUpTask, (task) -> {
        task.setDescription(task.description() + "\nDon't forget to tie your shoes!");
        return true;
    }); // will update the item in the selections it is already present in
    adaptable.updateFilters(wakeUpTask, (task) -> {
        boolean changed = task.assignee() != Person.MOM;
        task.setAssignee(Person.MOM);
        return changed;
    }); // will update the item and recalculate the predicates
    adaptable.updateReorder(wakeUpTask, (task) -> {
        final float ultimate = Float.POSITIVE_INFINITY;
        boolean changed = task.importance() != ultimate;
        task.setImportance(ultimate);
        return changed;
    }); // will fully recalculate the item placement
    
    // find the index of the first task assigned to "me", e.g. to scroll to it:
    int firstMyTask = adaptable.convertIndex(0, myTasksFilterId, allPassFilterId);

For incremental update broadcasting, see the `ElementObserver` interface, `Adaptation` and `TestUpdates`.

For more sophisticated examples, see the unit test package.

## Dependencies

Though originally developed for an Android environment, **Adaptable** is pure Java 1.7. A Gradle build script is provided but you can use a build system of your choice.

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

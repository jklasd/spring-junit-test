package com.github.jklasd.test.mvc.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

public class HeaderValueHolder {
	private final List<Object> values = new LinkedList<>();

	public void setValue(@Nullable Object value) {
		this.values.clear();
		if (value != null) {
			this.values.add(value);
		}
	}

	public void addValue(Object value) {
		this.values.add(value);
	}

	public void addValues(Collection<?> values) {
		this.values.addAll(values);
	}

	public void addValueArray(Object values) {
		CollectionUtils.mergeArrayIntoCollection(values, this.values);
	}

	public List<Object> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	public List<String> getStringValues() {
		List<String> stringList = new ArrayList<>(this.values.size());
		for (Object value : this.values) {
			stringList.add(value.toString());
		}
		return Collections.unmodifiableList(stringList);
	}

	@Nullable
	public Object getValue() {
		return (!this.values.isEmpty() ? this.values.get(0) : null);
	}

	@Nullable
	public String getStringValue() {
		return (!this.values.isEmpty() ? String.valueOf(this.values.get(0)) : null);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}

	/**
	 * Find a HeaderValueHolder by name, ignoring casing.
	 * 
	 * @param headers the Map of header names to HeaderValueHolders
	 * @param name    the name of the desired header
	 * @return the corresponding HeaderValueHolder, or {@code null} if none found
	 * @deprecated as of 5.1.10 in favor of using
	 *             {@link org.springframework.util.LinkedCaseInsensitiveMap}.
	 */
	@Nullable
	@Deprecated
	public HeaderValueHolder getByName(Map<String, HeaderValueHolder> headers, String name) {
		Assert.notNull(name, "Header name must not be null");
		for (Map.Entry<String, HeaderValueHolder> entry : headers.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}
}

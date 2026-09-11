package team.morpheus.launcher.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Deserializes ISO-8601 dates both with and without a UTC offset.
 * Dates without an offset (as found in NeoForge metadata) are treated as UTC.
 */
public class DateDeserializer implements JsonDeserializer<Date> {

    private static final Pattern COMPACT_OFFSET = Pattern.compile("([+-]\\d{2})(\\d{2})$");

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String value = json.getAsString();
        String normalizedValue = COMPACT_OFFSET.matcher(value).replaceFirst("$1:$2");

        try {
            return Date.from(OffsetDateTime.parse(normalizedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
        } catch (DateTimeParseException ignored) {
            // NeoForge dates do not contain an offset, so try the local ISO format below.
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException exception) {
            throw new JsonParseException("Unsupported date format: " + value, exception);
        }
    }
}

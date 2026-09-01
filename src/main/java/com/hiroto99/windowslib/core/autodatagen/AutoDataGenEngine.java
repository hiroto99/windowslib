package com.hiroto99.windowslib.core.autodatagen;

import com.hiroto99.windowslib.core.autodatagen.annotation.AddTag;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AutoDataGenEngine {
    // スキャンしたデータを一時的に蓄えておくプール（DataGenプロバイダがここを参照する）
    public static final List<DataEntryTag> COLLECTED_DATA_TAG = new ArrayList<>();

    // データの持ち運び用（Java record で簡潔に定義）
    public record DataEntryTag(AddTag annotation, Object value) {}

    public static List<DataEntryTag> scanPackage(String packageName) {
        // タグ生成アノテーションのスキャン
        COLLECTED_DATA_TAG.clear(); // 念のため初期化

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackage(packageName)
                        .addScanners(Scanners.FieldsAnnotated)
        );

        // @AutoDataGen が付与されたフィールドを全自動検出
        Set<Field> fields = reflections.getFieldsAnnotatedWith(AddTag.class);

        for (Field field : fields) {
            try {
                // static フィールドからオブジェクト（ItemやBlockのインスタンス）を取得
                Object value = field.get(null);
                AddTag ann = field.getAnnotation(AddTag.class);

                if (value != null) {
                    COLLECTED_DATA_TAG.add(new DataEntryTag(ann, value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return COLLECTED_DATA_TAG;
    }

    public void register(String packageName) {
        scanPackage(packageName);
    }
}

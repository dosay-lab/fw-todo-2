# DB設計書

## 1. 対象と基本方針

このアプリの保存先は MySQL（データを保存する仕組み）です。使用するテーブル（同じ種類のデータをまとめる表）は `todos` の1つだけで、やること1件を1行として保存します。ログイン、複数人での共有、削除後の復元、完了日時の記録は対象外です。画面の詳しい設計は別の設計書で扱います。

## 2. テーブル定義

`NOT NULL` は空の値を許さない設定、`NULL` は空の値を許す設定です。`DEFAULT` は値を指定しなかったときの初期値です。

| カラム（表の項目） | 型（値の種類） | 空の値 | 初期値・自動設定 | 内容 |
|---|---|---|---|---|
| `id` | `BIGINT`（大きな整数） | 不可 | 自動で連番を付ける | 1件を識別する番号。主キー（各行を一意に見分ける値） |
| `title` | `VARCHAR(255)`（最大255文字の文字列） | 不可 | なし | やることの名前 |
| `detail` | `VARCHAR(255)` | 可 | なし | メモ |
| `category` | `VARCHAR(255)` | 不可 | なし | ジャンル。指定された5種類から選ぶ |
| `priority` | `INT`（整数） | 不可 | `2` | 優先度。`1`＝高、`2`＝中、`3`＝低 |
| `due_date` | `DATE`（日付） | 可 | なし | 期限。未設定でもよい |
| `completed` | `BOOLEAN`（真偽値。はい／いいえ） | 不可 | `FALSE` | 完了状態。`FALSE`／`0`＝未完了、`TRUE`／`1`＝完了 |
| `created_at` | `DATETIME`（日付と時刻） | 不可 | 登録時に MySQL が現在日時を設定 | 登録日時 |
| `updated_at` | `DATETIME` | 不可 | 登録時に現在日時を設定し、更新時に MySQL が更新 | 最終更新日時 |

`category` の選択肢は「デザイン」「マーケティング」「プログラミング」「資格」「就職活動」です。`title`、`category`、`priority` は入力が必須です。`detail` と `due_date` は任意です。空白だけの `title` も入力なしとして扱います。これらの入力確認はアプリ側で行います。

## 3. 要件とカラムの対応

| 要件・操作 | 対応するカラム | 保存・利用方法 |
|---|---|---|
| やることを1件ずつ登録・識別 | `id` | 登録時に自動採番し、編集・削除の対象を特定する |
| やることの名前を登録・表示 | `title` | 必須、255文字以内。名前での部分一致検索にも使う |
| メモを登録・編集 | `detail` | 任意、255文字以内 |
| ジャンルを登録・表示 | `category` | 必須、指定の5種類。ジャンルでの完全一致検索にも使う |
| 優先度を登録・表示 | `priority` | 必須、1・2・3のいずれか。画面では高・中・低と表示する |
| 期限を登録・表示・並び替え | `due_date` | 任意。期限が近い順・遠い順の並び替えに使う |
| 未完了・完了を登録・編集・表示 | `completed` | 登録時は未完了。編集時に変更できる |
| 登録日時を自動で記録 | `created_at` | MySQL が登録時に設定する |
| 最終更新日時を自動で記録 | `updated_at` | MySQL が登録時と更新時に設定する |
| 一覧表示・削除 | 全カラム、特に `id` | 一覧は保存済みの行を読み、削除は `id` で対象行を選ぶ |

検索語、ジャンルの「すべて」、並び順、操作結果のメッセージは保存しません。これらは画面で使う一時的な値です。登録・編集・削除のログ（操作の記録）も、このテーブルには保存しません。

## 4. CREATE TABLE 文

DDL（テーブルを作るためのSQL文）は次のとおりです。`CHECK` は保存できる値を制限する条件です。MySQL 8.0.16 以降で `CHECK` が適用されます。

```sql
CREATE TABLE todos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255) NULL,
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_todos_category CHECK (
        category IN ('デザイン', 'マーケティング', 'プログラミング', '資格', '就職活動')
    ),
    CONSTRAINT chk_todos_priority CHECK (priority IN (1, 2, 3)),
    CONSTRAINT chk_todos_completed CHECK (completed IN (0, 1))
) CHARACTER SET utf8mb4;
```

`utf8mb4` は日本語などを保存するための文字設定です。`id`、`created_at`、`updated_at` は利用者が入力せず、MySQL が設定します。`priority` の初期値 `2` は保存時に値を省略した場合にだけ使い、画面では利用者に優先度を選んでもらいます。

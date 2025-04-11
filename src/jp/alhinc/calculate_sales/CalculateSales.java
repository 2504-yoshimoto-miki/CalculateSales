package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SEQUENCE = "売上ファイル名が連番になっていません";
	private static final String TOTAL_OVER_10_DIGITS = "合計金額が10桁を超えました";
	private static final String FILE_INVALID_CODE = "支店定義ファイルの支店コードが不正です";
	private static final String FILE_FORMAT_INVALID = "売り上げファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		//エラー処理3
			if (args.length != 1) {
				System.out.println(UNKNOWN_ERROR);
				return;
			}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//エラー処理2-1
		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));
			if ((latter - former) != 1) {
				System.out.println(FILE_NOT_SEQUENCE);
				return;
			}
		}

		//売上ファイルの読み込み(処理内容2-2)
		BufferedReader br = null;
		for (int i = 0; i < rcdFiles.size(); i++) {

			try {
				ArrayList<String> fileContents = new ArrayList<>();

				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;
				while ((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				//エラー処理2
				if (fileContents.size() != 2) {
					System.out.println(FILE_FORMAT_INVALID);
					return;
				}
				if (!branchNames.containsKey(fileContents.get(0))) {
					System.out.println(FILE_INVALID_CODE);
					return;
				}
				//エラー処理3
				if(!fileContents.get(1).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(fileContents.get(1));
				Long saleAmount = branchSales.get(fileContents.get(0)) + fileSale;
				//エラー処理2
				if (saleAmount >= 10000000000L) {
					System.out.println(TOTAL_OVER_10_DIGITS);
					return;
				}

				branchSales.put(fileContents.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			//デスクトップにあるテキストを File型のオブジェクトにする
			//エラー処理1
			File file = new File(path, fileName);
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				//エラー処理1
				if((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))){
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)

		BufferedWriter bw = null;
		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}

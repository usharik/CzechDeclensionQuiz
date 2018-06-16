package com.usharik.app;

/*
        Gson gson = new Gson();
        List<DocumentEntity> allDocuments = databaseManager.getActiveDbInstance().documentDao().getAllDocuments();
        for (DocumentEntity de : allDocuments) {
            WordInfo wordInfo = gson.fromJson(de.getJson(), WordInfo.class);
            wordInfo.word = wordInfo.word.replaceAll("[0-9]", "");
            if (wordInfo.translation.matches(".*\\,\\s")) {
                wordInfo.translation = wordInfo.translation.substring(0, wordInfo.translation.length() - 2);
            }
            for (int i=0; i<7; i++) {
                if (wordInfo.cases[0].length > i) {
                    wordInfo.cases[0][i] = wordInfo.cases[0][i] == null ? "" : wordInfo.cases[0][i].replaceAll("[0-9]", "");
                }
                if (wordInfo.cases[1].length > i) {
                    wordInfo.cases[1][i] = wordInfo.cases[1][i] == null ? "" : wordInfo.cases[1][i].replaceAll("[0-9]", "");
                }
            }
            de.setJson(gson.toJson(wordInfo));
            databaseManager.getActiveDbInstance().documentDao().updateDocument(de);
        }
        databaseManager.close();
 */

